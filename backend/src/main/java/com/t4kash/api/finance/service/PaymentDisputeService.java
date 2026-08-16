package com.t4kash.api.finance.service;

import com.t4kash.api.communication.service.NotificationService;
import com.t4kash.api.config.PaginationSupport;
import com.t4kash.api.exception.ForbiddenOperationException;
import com.t4kash.api.exception.ResourceConflictException;
import com.t4kash.api.exception.ResourceNotFoundException;
import com.t4kash.api.finance.dto.CreatePaymentDisputeRequest;
import com.t4kash.api.finance.dto.PaymentDisputeResponse;
import com.t4kash.api.finance.dto.ResolvePaymentDisputeRequest;
import com.t4kash.api.finance.entity.DisputaPago;
import com.t4kash.api.finance.entity.EventoDisputaPago;
import com.t4kash.api.finance.entity.Pago;
import com.t4kash.api.finance.repository.DisputaPagoRepository;
import com.t4kash.api.finance.repository.EventoDisputaPagoRepository;
import com.t4kash.api.finance.repository.PagoRepository;
import com.t4kash.api.marketplace.entity.TrabajoAsignado;
import com.t4kash.api.marketplace.repository.TrabajoAsignadoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class PaymentDisputeService {
    public static final String PAYMENT_DISPUTED = PaymentService.PAYMENT_DISPUTED;
    public static final String PAYMENT_REFUNDED = PaymentService.PAYMENT_REFUNDED;
    private static final List<String> ACTIVE_STATES = List.of(
            "ABIERTA", "NEGOCIACION", "ESPERANDO_EVIDENCIA", "EN_REVISION"
    );

    private final PagoRepository paymentRepository;
    private final DisputaPagoRepository disputeRepository;
    private final EventoDisputaPagoRepository eventRepository;
    private final TrabajoAsignadoRepository jobRepository;
    private final FinancialLedgerService ledgerService;
    private final NotificationService notificationService;

    public PaymentDisputeService(
            PagoRepository paymentRepository,
            DisputaPagoRepository disputeRepository,
            EventoDisputaPagoRepository eventRepository,
            TrabajoAsignadoRepository jobRepository,
            FinancialLedgerService ledgerService,
            NotificationService notificationService
    ) {
        this.paymentRepository = paymentRepository;
        this.disputeRepository = disputeRepository;
        this.eventRepository = eventRepository;
        this.jobRepository = jobRepository;
        this.ledgerService = ledgerService;
        this.notificationService = notificationService;
    }

    @Transactional
    public PaymentDisputeResponse open(
            Integer currentUserId,
            Integer paymentId,
            CreatePaymentDisputeRequest request
    ) {
        Pago payment = paymentRepository.findByIdForUpdate(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("El pago indicado no existe."));
        requireParticipant(payment, currentUserId);
        if (!PaymentService.METHOD_PAGADITO.equals(payment.getMetodoPago())
                || !"SANDBOX".equals(payment.getEntornoPago())) {
            throw new ResourceConflictException(
                    "Las disputas financieras solo estan disponibles para pagos protegidos Sandbox."
            );
        }
        if (!PaymentService.FUNDS_HELD.equals(payment.getEstadoPago())) {
            throw new ResourceConflictException(
                    "Solo puedes disputar un pago mientras sus fondos esten retenidos."
            );
        }
        if (disputeRepository.existsByIdPagoAndEstadoDisputaIn(paymentId, ACTIVE_STATES)) {
            throw new ResourceConflictException("Este pago ya tiene una disputa activa.");
        }

        LocalDateTime now = LocalDateTime.now();
        DisputaPago dispute = new DisputaPago();
        dispute.setIdPago(paymentId);
        dispute.setIdUsuarioAbre(currentUserId);
        dispute.setMotivo(request.motivo().trim());
        dispute.setDescripcion(request.descripcion().trim());
        dispute.setSolucionSolicitada(request.solucionSolicitada());
        dispute.setMontoDisputado(payment.getMontoTotalCliente());
        dispute.setEstadoDisputa("ABIERTA");
        dispute.setPrioridad("NORMAL");
        dispute.setFechaApertura(now);
        dispute.setFechaLimiteRespuesta(now.plusDays(3));
        dispute.setFechaActualizacion(now);
        DisputaPago saved = disputeRepository.save(dispute);

        payment.setEstadoPago(PAYMENT_DISPUTED);
        payment.setFechaActualizacion(now);
        paymentRepository.save(payment);
        recordEvent(saved, currentUserId, "APERTURA", null, "ABIERTA", request.descripcion());
        ledgerService.recordMovement(
                payment,
                currentUserId,
                "DISPUTA_ABIERTA",
                "CONGELADO",
                payment.getMontoEstudiante(),
                "Fondos congelados por la disputa #" + saved.getIdDisputa(),
                "DISPUTE:OPEN:" + saved.getIdDisputa()
        );
        notifyCounterpart(payment, currentUserId, "Pago en disputa",
                "El pago del trabajo #" + payment.getIdTrabajo() + " fue congelado para revision.");
        return PaymentDisputeResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public List<PaymentDisputeResponse> listForUser(Integer currentUserId, int page, int size) {
        return disputeRepository.findVisibleToUser(
                        currentUserId,
                        PaginationSupport.page(page, size)
                ).stream()
                .map(PaymentDisputeResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PaymentDisputeResponse> listActiveForAdmin(int page, int size) {
        return disputeRepository.findByEstadoDisputaInOrderByFechaAperturaAsc(
                        ACTIVE_STATES,
                        PaginationSupport.page(page, size)
                )
                .stream()
                .map(PaymentDisputeResponse::fromEntity)
                .toList();
    }

    @Transactional
    public PaymentDisputeResponse resolve(
            Integer adminId,
            Integer disputeId,
            ResolvePaymentDisputeRequest request
    ) {
        DisputaPago dispute = disputeRepository.findByIdForUpdate(disputeId)
                .orElseThrow(() -> new ResourceNotFoundException("La disputa indicada no existe."));
        if (!ACTIVE_STATES.contains(dispute.getEstadoDisputa())) {
            throw new ResourceConflictException("Esta disputa ya fue resuelta.");
        }
        Pago payment = paymentRepository.findByIdForUpdate(dispute.getIdPago())
                .orElseThrow(() -> new ResourceNotFoundException("El pago de la disputa no existe."));
        if (!PAYMENT_DISPUTED.equals(payment.getEstadoPago())) {
            throw new ResourceConflictException("El pago ya no se encuentra congelado por disputa.");
        }
        TrabajoAsignado job = jobRepository.findByIdForUpdate(payment.getIdTrabajo())
                .orElseThrow(() -> new ResourceNotFoundException("El trabajo de la disputa no existe."));

        LocalDateTime now = LocalDateTime.now();
        String previousState = dispute.getEstadoDisputa();
        dispute.setIdAdminAsignado(adminId);
        dispute.setResolucion(request.resolucion().trim());
        dispute.setFechaResolucion(now);
        dispute.setFechaActualizacion(now);

        if ("LIBERAR_ESTUDIANTE".equals(request.decision())) {
            dispute.setEstadoDisputa("RESUELTA_ESTUDIANTE");
            payment.setEstadoPago(PaymentService.PAYMENT_RELEASED);
            payment.setFechaLiberacion(now);
            job.setEstadoTrabajo("FINALIZADO");
            ledgerService.registerSandboxPayout(payment);
            ledgerService.recordMovement(
                    payment, payment.getIdEstudiante(), "PAGO_LIBERADO_DISPUTA", "DISPONIBLE",
                    payment.getMontoEstudiante(), "Pago liberado por resolucion administrativa",
                    "DISPUTE:RELEASE:" + disputeId
            );
        } else {
            dispute.setEstadoDisputa("RESUELTA_CLIENTE");
            payment.setEstadoPago(PAYMENT_REFUNDED);
            payment.setFechaReembolso(now);
            job.setEstadoTrabajo("CANCELADO_DISPUTA");
            ledgerService.registerSandboxRefund(payment, dispute, adminId, request.resolucion());
            ledgerService.recordMovement(
                    payment, payment.getIdCliente(), "REEMBOLSO_CONFIRMADO", "REEMBOLSADO",
                    payment.getMontoTotalCliente(), "Reembolso Sandbox por resolucion administrativa",
                    "DISPUTE:REFUND:" + disputeId
            );
        }

        payment.setFechaActualizacion(now);
        paymentRepository.save(payment);
        jobRepository.save(job);
        DisputaPago saved = disputeRepository.save(dispute);
        recordEvent(
                saved, adminId, "RESOLUCION", previousState, saved.getEstadoDisputa(),
                request.resolucion()
        );
        notificationService.create(
                payment.getIdCliente(), "Disputa resuelta",
                "La disputa del trabajo #" + payment.getIdTrabajo() + " ya tiene una resolucion."
        );
        notificationService.create(
                payment.getIdEstudiante(), "Disputa resuelta",
                "La disputa del trabajo #" + payment.getIdTrabajo() + " ya tiene una resolucion."
        );
        return PaymentDisputeResponse.fromEntity(saved);
    }

    private void recordEvent(
            DisputaPago dispute,
            Integer userId,
            String type,
            String previousState,
            String newState,
            String detail
    ) {
        EventoDisputaPago event = new EventoDisputaPago();
        event.setIdDisputa(dispute.getIdDisputa());
        event.setIdUsuario(userId);
        event.setTipoEvento(type);
        event.setEstadoAnterior(previousState);
        event.setEstadoNuevo(newState);
        event.setDetalle(detail);
        event.setFechaEvento(LocalDateTime.now());
        eventRepository.save(event);
    }

    private void requireParticipant(Pago payment, Integer userId) {
        if (!payment.getIdCliente().equals(userId) && !payment.getIdEstudiante().equals(userId)) {
            throw new ForbiddenOperationException("No puedes disputar este pago.");
        }
    }

    private void notifyCounterpart(Pago payment, Integer userId, String title, String message) {
        Integer counterpart = payment.getIdCliente().equals(userId)
                ? payment.getIdEstudiante()
                : payment.getIdCliente();
        notificationService.create(counterpart, title, message);
    }
}
