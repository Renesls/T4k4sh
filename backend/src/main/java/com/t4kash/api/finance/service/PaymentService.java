package com.t4kash.api.finance.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.t4kash.api.communication.service.NotificationService;
import com.t4kash.api.exception.ForbiddenOperationException;
import com.t4kash.api.exception.PaymentProviderException;
import com.t4kash.api.exception.ResourceConflictException;
import com.t4kash.api.exception.ResourceNotFoundException;
import com.t4kash.api.finance.dto.CheckoutResponse;
import com.t4kash.api.finance.dto.PaymentResponse;
import com.t4kash.api.finance.dto.WalletMovementResponse;
import com.t4kash.api.finance.dto.WalletResponse;
import com.t4kash.api.finance.entity.EventoWebhookPago;
import com.t4kash.api.finance.entity.Pago;
import com.t4kash.api.finance.entity.TransaccionPago;
import com.t4kash.api.finance.repository.EventoWebhookPagoRepository;
import com.t4kash.api.finance.repository.PagoRepository;
import com.t4kash.api.finance.repository.TransaccionPagoRepository;
import com.t4kash.api.marketplace.entity.Postulacion;
import com.t4kash.api.marketplace.entity.Tarea;
import com.t4kash.api.marketplace.entity.TrabajoAsignado;
import com.t4kash.api.marketplace.repository.TrabajoAsignadoRepository;
import com.t4kash.api.marketplace.service.TaskService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class PaymentService {
    public static final String METHOD_PAGADITO = "PAGADITO";
    public static final String METHOD_CASH = "EFECTIVO";
    public static final String PAYMENT_PENDING = "PENDIENTE_PAGO";
    public static final String FUNDS_HELD = "FONDOS_RETENIDOS";
    public static final String PAYMENT_RELEASED = "PAGO_LIBERADO";
    public static final String EXTERNAL_PENDING = "PAGO_EXTERNO_PENDIENTE";
    public static final String EXTERNAL_CONFIRMED = "PAGO_EXTERNO_CONFIRMADO";
    public static final String JOB_CASH_CONFIRMATION_PENDING = "PAGO_EFECTIVO_PENDIENTE";
    private static final String JOB_FINISHED = "FINALIZADO";

    private static final Set<String> RETRYABLE_STATES = Set.of(
            PAYMENT_PENDING, "PAGO_FALLIDO", "PAGO_CANCELADO", "PAGO_EXPIRADO"
    );
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

    private final PagoRepository paymentRepository;
    private final TransaccionPagoRepository movementRepository;
    private final EventoWebhookPagoRepository webhookRepository;
    private final TrabajoAsignadoRepository jobRepository;
    private final TaskService taskService;
    private final PagaditoClient pagaditoClient;
    private final PagaditoWebhookVerifier webhookVerifier;
    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;
    private final BigDecimal clientFeePercent;
    private final BigDecimal studentFeePercent;
    private final BigDecimal processorFeePercent;
    private final BigDecimal processorFixedFee;
    private final BigDecimal processorTaxPercent;
    private final String environment;
    private final String publicBaseUrl;

    public PaymentService(
            PagoRepository paymentRepository,
            TransaccionPagoRepository movementRepository,
            EventoWebhookPagoRepository webhookRepository,
            TrabajoAsignadoRepository jobRepository,
            TaskService taskService,
            PagaditoClient pagaditoClient,
            PagaditoWebhookVerifier webhookVerifier,
            NotificationService notificationService,
            ObjectMapper objectMapper,
            @Value("${app.payments.client-fee-percent:10.00}") BigDecimal clientFeePercent,
            @Value("${app.payments.student-fee-percent:5.00}") BigDecimal studentFeePercent,
            @Value("${app.pagadito.processor-fee-percent:0}") BigDecimal processorFeePercent,
            @Value("${app.pagadito.processor-fixed-fee:0}") BigDecimal processorFixedFee,
            @Value("${app.pagadito.processor-tax-percent:0}") BigDecimal processorTaxPercent,
            @Value("${app.pagadito.environment:SANDBOX}") String environment,
            @Value("${app.public-base-url:http://localhost:8080}") String publicBaseUrl
    ) {
        this.paymentRepository = paymentRepository;
        this.movementRepository = movementRepository;
        this.webhookRepository = webhookRepository;
        this.jobRepository = jobRepository;
        this.taskService = taskService;
        this.pagaditoClient = pagaditoClient;
        this.webhookVerifier = webhookVerifier;
        this.notificationService = notificationService;
        this.objectMapper = objectMapper;
        this.clientFeePercent = clientFeePercent;
        this.studentFeePercent = studentFeePercent;
        this.processorFeePercent = processorFeePercent;
        this.processorFixedFee = processorFixedFee;
        this.processorTaxPercent = processorTaxPercent;
        this.environment = environment.toUpperCase(Locale.ROOT);
        this.publicBaseUrl = publicBaseUrl.replaceAll("/+$", "");
    }

    @Transactional
    public PaymentResponse createForAcceptedApplication(
            TrabajoAsignado job,
            Tarea task,
            Postulacion application,
            String requestedMethod
    ) {
        String method = normalizeMethod(requestedMethod);
        if (METHOD_CASH.equals(method) && !"PRESENCIAL".equals(task.getModalidad())) {
            throw new IllegalArgumentException(
                    "El pago en efectivo solo esta disponible para tareas presenciales."
            );
        }
        BigDecimal agreedAmount = money(
                application.getPrecioPropuesto() == null
                        ? task.getPresupuesto()
                        : application.getPrecioPropuesto()
        );
        if (agreedAmount.signum() <= 0) {
            throw new IllegalArgumentException("El monto acordado debe ser mayor que cero.");
        }

        // El take rate se reparte entre las dos partes: el cliente paga su porcentaje
        // por encima del precio acordado y al estudiante se le retiene el suyo del pago.
        boolean cash = METHOD_CASH.equals(method);
        BigDecimal clientPercent = cash ? BigDecimal.ZERO : clientFeePercent;
        BigDecimal studentPercent = cash ? BigDecimal.ZERO : studentFeePercent;
        BigDecimal feePercent = clientPercent.add(studentPercent);
        BigDecimal clientFee = percentage(agreedAmount, clientPercent);
        BigDecimal studentFee = percentage(agreedAmount, studentPercent);
        BigDecimal studentAmount = money(agreedAmount.subtract(studentFee));
        BigDecimal platformFee = money(clientFee.add(studentFee));
        BigDecimal processorFee = cash
                ? BigDecimal.ZERO
                : money(percentage(agreedAmount, processorFeePercent).add(processorFixedFee));
        BigDecimal processorTax = cash
                ? BigDecimal.ZERO
                : percentage(processorFee, processorTaxPercent);
        BigDecimal total = money(agreedAmount.add(clientFee).add(processorFee).add(processorTax));
        LocalDateTime now = LocalDateTime.now();

        Pago payment = new Pago();
        payment.setIdTrabajo(job.getIdTrabajo());
        payment.setIdCliente(task.getIdCliente());
        payment.setIdEstudiante(application.getIdEstudiante());
        payment.setUuidPago(UUID.randomUUID());
        payment.setProveedorPago(cash ? "EXTERNO" : METHOD_PAGADITO);
        payment.setEntornoPago(cash ? "EXTERNO" : environment);
        payment.setMetodoPago(method);
        payment.setMonedaCobro("NIO");
        payment.setMonedaProcesamiento(cash ? null : "NIO");
        payment.setMontoProcesamiento(cash ? null : total);
        payment.setMontoEstudiante(studentAmount);
        payment.setPorcentajeComisionPlataforma(feePercent);
        payment.setComisionPlataforma(platformFee);
        payment.setComisionProcesador(processorFee);
        payment.setImpuestoProcesador(processorTax);
        payment.setMontoTotalCliente(total);
        payment.setEstadoPago(cash ? EXTERNAL_PENDING : PAYMENT_PENDING);
        payment.setReferenciaComercio(newCommerceReference());
        payment.setFechaCreacion(now);
        payment.setFechaActualizacion(now);
        Pago saved = paymentRepository.save(payment);

        recordMovement(
                saved,
                task.getIdCliente(),
                "PAGO_CREADO",
                cash ? "EXTERNO" : "PENDIENTE",
                total,
                "Pago creado para el trabajo #" + job.getIdTrabajo(),
                "PAYMENT_CREATED:" + saved.getUuidPago(),
                null
        );
        return PaymentResponse.fromEntity(saved, task.getIdCliente());
    }

    @Transactional(readOnly = true)
    public PaymentResponse findByJob(Integer currentUserId, Integer jobId) {
        Pago payment = findPaymentByJob(jobId);
        requireParticipant(payment, currentUserId);
        return PaymentResponse.fromEntity(payment, currentUserId);
    }

    @Transactional(readOnly = true)
    public WalletResponse getWallet(Integer currentUserId) {
        List<Pago> payments = paymentRepository.findVisibleToUser(currentUserId);
        List<WalletMovementResponse> movements = movementRepository
                .findTop30ByIdUsuarioOrderByFechaRegistroDesc(currentUserId)
                .stream()
                .map(WalletMovementResponse::fromEntity)
                .toList();
        BigDecimal available = paymentRepository.sumStudentAmountByStatus(
                currentUserId,
                PAYMENT_RELEASED
        );
        BigDecimal held = paymentRepository.sumStudentAmountByStatus(
                currentUserId,
                FUNDS_HELD
        );
        BigDecimal earned = paymentRepository.sumStudentAmountByStatuses(
                currentUserId,
                List.of(PAYMENT_RELEASED, EXTERNAL_CONFIRMED)
        );
        return new WalletResponse(
                "NIO",
                money(available),
                money(held),
                money(earned),
                payments.stream().map(payment ->
                        PaymentResponse.fromEntity(payment, currentUserId)
                ).toList(),
                movements
        );
    }

    @Transactional
    public CheckoutResponse createCheckout(Integer currentUserId, Integer jobId) {
        Pago payment = findPaymentByJob(jobId);
        if (!payment.getIdCliente().equals(currentUserId)) {
            throw new ForbiddenOperationException("Solo el cliente puede iniciar este pago.");
        }
        if (!METHOD_PAGADITO.equals(payment.getMetodoPago())) {
            throw new ResourceConflictException("Este trabajo fue acordado con pago en efectivo.");
        }
        if (!RETRYABLE_STATES.contains(payment.getEstadoPago())) {
            throw new ResourceConflictException("Este pago ya fue procesado o esta siendo verificado.");
        }
        if (PAYMENT_PENDING.equals(payment.getEstadoPago())
                && payment.getReferenciaProveedor() != null
                && payment.getFechaExpiracion() != null
                && payment.getFechaExpiracion().isAfter(LocalDateTime.now())) {
            throw new ResourceConflictException(
                    "Ya existe un pago abierto. Espera a que finalice o actualiza su estado."
            );
        }

        Tarea task = taskService.findTaskEntity(
                jobRepository.findById(jobId)
                        .orElseThrow(() -> new ResourceNotFoundException("El trabajo indicado no existe."))
                        .getIdTarea()
        );
        payment.setReferenciaComercio(newCommerceReference());
        PagaditoClient.Checkout checkout = pagaditoClient.checkout(
                payment.getReferenciaComercio(),
                payment.getMontoTotalCliente(),
                "Trabajo T4KASH: " + task.getTitulo(),
                publicBaseUrl + "/api/tasks/" + task.getIdTarea()
        );
        payment.setReferenciaProveedor(checkout.transactionToken());
        payment.setEstadoPago(PAYMENT_PENDING);
        payment.setFechaExpiracion(LocalDateTime.now().plusMinutes(10));
        payment.setFechaActualizacion(LocalDateTime.now());
        paymentRepository.save(payment);
        return new CheckoutResponse(payment.getIdPago(), checkout.url(), payment.getEstadoPago());
    }

    @Transactional
    public PaymentResponse refreshStatus(Integer currentUserId, Integer paymentId) {
        Pago payment = findPayment(paymentId);
        requireParticipant(payment, currentUserId);
        if (!METHOD_PAGADITO.equals(payment.getMetodoPago())) {
            return PaymentResponse.fromEntity(payment, currentUserId);
        }
        if (payment.getReferenciaProveedor() == null || payment.getReferenciaProveedor().isBlank()) {
            throw new ResourceConflictException("Este pago aun no tiene una transaccion en Pagadito.");
        }
        applyProviderStatus(
                payment,
                pagaditoClient.getStatus(payment.getReferenciaProveedor()),
                "STATUS_REFRESH:" + payment.getReferenciaProveedor()
        );
        return PaymentResponse.fromEntity(paymentRepository.save(payment), currentUserId);
    }

    @Transactional
    public String processReturn(String transactionToken) {
        Pago payment = paymentRepository.findByReferenciaProveedor(transactionToken)
                .orElseThrow(() -> new ResourceNotFoundException("El pago devuelto por Pagadito no existe."));
        applyProviderStatus(
                payment,
                pagaditoClient.getStatus(transactionToken),
                "RETURN:" + transactionToken
        );
        paymentRepository.save(payment);
        return payment.getEstadoPago();
    }

    @Transactional(noRollbackFor = IllegalArgumentException.class)
    public void processWebhook(
            String rawBody,
            PagaditoWebhookVerifier.Headers headers
    ) {
        JsonNode root = readJson(rawBody);
        String eventId = requiredText(root, "id");
        String eventType = requiredText(root, "event_type");
        if (webhookRepository.existsByProveedorPagoAndEntornoPagoAndIdEventoProveedor(
                METHOD_PAGADITO,
                environment,
                eventId
        )) {
            return;
        }
        boolean validSignature = webhookVerifier.verify(rawBody, headers, eventId);
        JsonNode resource = root.path("resource");
        String commerceReference = requiredText(resource, "ern");
        Pago payment = paymentRepository.findByReferenciaComercio(commerceReference).orElse(null);
        EventoWebhookPago event = new EventoWebhookPago();
        event.setIdPago(payment == null ? null : payment.getIdPago());
        event.setProveedorPago(METHOD_PAGADITO);
        event.setEntornoPago(environment);
        event.setIdEventoProveedor(eventId);
        event.setTipoEvento(eventType);
        event.setFirmaValida(validSignature);
        event.setPayload(objectMapper.convertValue(root, new TypeReference<>() { }));
        event.setEstadoProcesamiento(validSignature ? "PENDIENTE" : "RECHAZADO");
        event.setIntentosProcesamiento(1);
        event.setFechaRecepcion(LocalDateTime.now());
        if (!validSignature) {
            event.setUltimoError("Firma no valida.");
            webhookRepository.save(event);
            throw new IllegalArgumentException("La firma del webhook de Pagadito no es valida.");
        }
        if (payment == null) {
            event.setEstadoProcesamiento("ERROR");
            event.setUltimoError("No existe un pago para el ERN recibido.");
            webhookRepository.save(event);
            throw new ResourceNotFoundException("No existe un pago para el evento recibido.");
        }
        validateWebhookAmount(payment, resource.path("amount"));
        payment.setReferenciaProveedor(requiredText(resource, "token"));
        applyProviderStatus(
                payment,
                new PagaditoClient.TransactionStatus(
                        requiredText(resource, "status").toUpperCase(Locale.ROOT),
                        resource.path("reference").asText(null),
                        resource.path("update_timestamp").asText(null)
                ),
                "WEBHOOK:" + eventId
        );
        paymentRepository.save(payment);
        event.setEstadoProcesamiento("PROCESADO");
        event.setFechaProcesamiento(LocalDateTime.now());
        webhookRepository.save(event);
    }

    @Transactional
    public boolean releaseForApprovedDelivery(TrabajoAsignado job) {
        Pago payment = findPaymentByJob(job.getIdTrabajo());
        LocalDateTime now = LocalDateTime.now();
        if (METHOD_PAGADITO.equals(payment.getMetodoPago())) {
            if (!FUNDS_HELD.equals(payment.getEstadoPago())) {
                throw new ResourceConflictException(
                        "No se puede aprobar la entrega sin un pago protegido confirmado."
                );
            }
            payment.setEstadoPago(PAYMENT_RELEASED);
            payment.setFechaLiberacion(now);
            recordMovement(
                    payment,
                    payment.getIdEstudiante(),
                    "PAGO_LIBERADO",
                    "DISPONIBLE",
                    payment.getMontoEstudiante(),
                    "Pago liberado por entrega aprobada",
                    "RELEASE:" + payment.getUuidPago(),
                    payment.getReferenciaProveedor()
            );
            payment.setFechaActualizacion(now);
            paymentRepository.save(payment);
            return true;
        }

        if (!EXTERNAL_PENDING.equals(payment.getEstadoPago())) {
            throw new ResourceConflictException(
                    "El pago en efectivo ya fue confirmado o no esta pendiente."
            );
        }
        return false;
    }

    @Transactional
    public PaymentResponse confirmCashReceipt(Integer currentUserId, Integer jobId) {
        Pago payment = paymentRepository.findByIdTrabajoForUpdate(jobId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "El trabajo aun no tiene un pago asociado."
                ));
        if (!payment.getIdEstudiante().equals(currentUserId)) {
            throw new ForbiddenOperationException(
                    "Solo el estudiante asignado puede confirmar que recibio el efectivo."
            );
        }
        if (!METHOD_CASH.equals(payment.getMetodoPago())) {
            throw new ResourceConflictException("Este trabajo no utiliza pago en efectivo.");
        }
        if (!EXTERNAL_PENDING.equals(payment.getEstadoPago())) {
            throw new ResourceConflictException("El pago en efectivo ya fue confirmado.");
        }

        TrabajoAsignado job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "El trabajo asociado al pago no existe."
                ));
        if (!JOB_CASH_CONFIRMATION_PENDING.equals(job.getEstadoTrabajo())) {
            throw new ResourceConflictException(
                    "El cliente aun no ha aprobado la entrega y declarado el pago."
            );
        }

        LocalDateTime now = LocalDateTime.now();
        payment.setEstadoPago(EXTERNAL_CONFIRMED);
        payment.setFechaConfirmacion(now);
        payment.setFechaLiberacion(now);
        payment.setFechaActualizacion(now);
        paymentRepository.save(payment);
        job.setEstadoTrabajo(JOB_FINISHED);
        jobRepository.save(job);
        recordMovement(
                payment,
                currentUserId,
                "PAGO_EFECTIVO_CONFIRMADO",
                "EXTERNO",
                payment.getMontoEstudiante(),
                "Pago en efectivo confirmado por el estudiante",
                "CASH_CONFIRMED:" + payment.getUuidPago(),
                null
        );
        notificationService.create(
                payment.getIdCliente(),
                "Pago en efectivo confirmado",
                "El estudiante confirmo el pago del trabajo #" + jobId + "."
        );
        return PaymentResponse.fromEntity(payment, currentUserId);
    }

    private void applyProviderStatus(
            Pago payment,
            PagaditoClient.TransactionStatus providerStatus,
            String idempotencyKey
    ) {
        String status = providerStatus.status();
        LocalDateTime now = LocalDateTime.now();
        switch (status) {
            case "COMPLETED" -> confirmProtectedPayment(payment, providerStatus.reference(), idempotencyKey);
            case "REGISTERED" -> payment.setEstadoPago("PAGO_REGISTRADO");
            case "VERIFYING" -> payment.setEstadoPago("PAGO_EN_VERIFICACION");
            case "FAILED" -> payment.setEstadoPago("PAGO_FALLIDO");
            case "CANCELED" -> payment.setEstadoPago("PAGO_CANCELADO");
            case "EXPIRED" -> payment.setEstadoPago("PAGO_EXPIRADO");
            case "REVOKED" -> payment.setEstadoPago("PAGO_REVOCADO");
            default -> throw new PaymentProviderException("Pagadito devolvio el estado " + status + ".");
        }
        payment.setFechaActualizacion(now);
    }

    private void confirmProtectedPayment(Pago payment, String providerReference, String idempotencyKey) {
        if (FUNDS_HELD.equals(payment.getEstadoPago()) || PAYMENT_RELEASED.equals(payment.getEstadoPago())) {
            return;
        }
        payment.setEstadoPago(FUNDS_HELD);
        payment.setFechaConfirmacion(LocalDateTime.now());
        TrabajoAsignado job = jobRepository.findById(payment.getIdTrabajo())
                .orElseThrow(() -> new ResourceNotFoundException("El trabajo asociado al pago no existe."));
        job.setEstadoTrabajo("EN_PROCESO");
        jobRepository.save(job);
        recordMovement(
                payment,
                payment.getIdEstudiante(),
                "FONDOS_RETENIDOS",
                "RETENIDO",
                payment.getMontoEstudiante(),
                "Pago protegido confirmado; fondos retenidos hasta aprobar la entrega",
                idempotencyKey,
                providerReference
        );
        notificationService.create(
                payment.getIdEstudiante(),
                "Pago protegido confirmado",
                "Ya puedes iniciar el trabajo #" + payment.getIdTrabajo() + "."
        );
    }

    private void recordMovement(
            Pago payment,
            Integer userId,
            String type,
            String affectedBalance,
            BigDecimal amount,
            String description,
            String idempotencyKey,
            String providerReference
    ) {
        if (movementRepository.existsByClaveIdempotencia(idempotencyKey)) {
            return;
        }
        TransaccionPago movement = new TransaccionPago();
        movement.setIdPago(payment.getIdPago());
        movement.setIdUsuario(userId);
        movement.setTipoMovimiento(type);
        movement.setSaldoAfectado(affectedBalance);
        movement.setMonto(money(amount));
        movement.setMoneda("NIO");
        movement.setEstadoMovimiento("REGISTRADO");
        movement.setProveedorPago(payment.getProveedorPago());
        movement.setClaveIdempotencia(idempotencyKey);
        movement.setReferenciaProveedor(providerReference);
        movement.setFechaEventoProveedor(providerReference == null ? null : LocalDateTime.now());
        movement.setFechaRegistro(LocalDateTime.now());
        movement.setDescripcion(description);
        movement.setMetadatos(Map.of("idTrabajo", payment.getIdTrabajo()));
        movementRepository.save(movement);
    }

    private void validateWebhookAmount(Pago payment, JsonNode amount) {
        BigDecimal received = new BigDecimal(requiredText(amount, "total"));
        String currency = requiredText(amount, "currency");
        if (money(received).compareTo(payment.getMontoTotalCliente()) != 0
                || !payment.getMonedaCobro().equalsIgnoreCase(currency)) {
            throw new IllegalArgumentException("El monto del webhook no coincide con el pago registrado.");
        }
    }

    private Pago findPayment(Integer paymentId) {
        return paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("El pago indicado no existe."));
    }

    private Pago findPaymentByJob(Integer jobId) {
        return paymentRepository.findByIdTrabajo(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("El trabajo aun no tiene un pago asociado."));
    }

    private void requireParticipant(Pago payment, Integer currentUserId) {
        if (!payment.getIdCliente().equals(currentUserId)
                && !payment.getIdEstudiante().equals(currentUserId)) {
            throw new ForbiddenOperationException("No puedes consultar este pago.");
        }
    }

    private String normalizeMethod(String value) {
        String method = value == null ? METHOD_PAGADITO : value.trim().toUpperCase(Locale.ROOT);
        if (!METHOD_PAGADITO.equals(method) && !METHOD_CASH.equals(method)) {
            throw new IllegalArgumentException("El metodo de pago debe ser PAGADITO o EFECTIVO.");
        }
        return method;
    }

    private String newCommerceReference() {
        return "T4KASH-" + UUID.randomUUID().toString().replace("-", "").toUpperCase(Locale.ROOT);
    }

    private BigDecimal percentage(BigDecimal amount, BigDecimal percent) {
        return money(amount.multiply(percent).divide(ONE_HUNDRED, 8, RoundingMode.HALF_UP));
    }

    private BigDecimal money(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP);
    }

    private JsonNode readJson(String rawBody) {
        try {
            return objectMapper.readTree(rawBody);
        } catch (Exception exception) {
            throw new IllegalArgumentException("El webhook no contiene un JSON valido.");
        }
    }

    private String requiredText(JsonNode node, String field) {
        String value = node.path(field).asText();
        if (value.isBlank()) {
            throw new IllegalArgumentException("El webhook no contiene " + field + ".");
        }
        return value;
    }
}
