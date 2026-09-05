package com.t4kash.api.finance.service;

import com.t4kash.api.communication.service.NotificationService;
import com.t4kash.api.exception.ForbiddenOperationException;
import com.t4kash.api.exception.ResourceConflictException;
import com.t4kash.api.finance.dto.CreatePaymentDisputeRequest;
import com.t4kash.api.finance.dto.PaymentDisputeResponse;
import com.t4kash.api.finance.dto.ResolvePaymentDisputeRequest;
import com.t4kash.api.finance.entity.DisputaPago;
import com.t4kash.api.finance.entity.Pago;
import com.t4kash.api.finance.repository.DisputaPagoRepository;
import com.t4kash.api.finance.repository.EventoDisputaPagoRepository;
import com.t4kash.api.finance.repository.PagoRepository;
import com.t4kash.api.marketplace.entity.TrabajoAsignado;
import com.t4kash.api.marketplace.repository.TrabajoAsignadoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PaymentDisputeServiceTest {
    private PagoRepository paymentRepository;
    private DisputaPagoRepository disputeRepository;
    private EventoDisputaPagoRepository eventRepository;
    private TrabajoAsignadoRepository jobRepository;
    private FinancialLedgerService ledgerService;
    private PaymentDisputeService service;

    @BeforeEach
    void setUp() {
        paymentRepository = mock(PagoRepository.class);
        disputeRepository = mock(DisputaPagoRepository.class);
        eventRepository = mock(EventoDisputaPagoRepository.class);
        jobRepository = mock(TrabajoAsignadoRepository.class);
        ledgerService = mock(FinancialLedgerService.class);
        service = new PaymentDisputeService(
                paymentRepository,
                disputeRepository,
                eventRepository,
                jobRepository,
                ledgerService,
                mock(NotificationService.class)
        );
        when(disputeRepository.save(any(DisputaPago.class))).thenAnswer(invocation -> {
            DisputaPago dispute = invocation.getArgument(0);
            if (dispute.getIdDisputa() == null) {
                dispute.setIdDisputa(3);
            }
            return dispute;
        });
    }

    @Test
    void opensDisputeAndFreezesProtectedFunds() {
        Pago payment = payment("FONDOS_RETENIDOS");
        when(paymentRepository.findByIdForUpdate(8)).thenReturn(Optional.of(payment));

        PaymentDisputeResponse response = service.open(
                10,
                8,
                new CreatePaymentDisputeRequest(
                        "Entrega incompleta",
                        "La entrega no coincide con el acuerdo establecido.",
                        "REEMBOLSO_CLIENTE"
                )
        );

        assertEquals("EN_DISPUTA", payment.getEstadoPago());
        assertEquals("ABIERTA", response.estadoDisputa());
        assertEquals(new BigDecimal("101.00"), response.montoDisputado());
        assertNotNull(response.fechaLimiteRespuesta());
        verify(eventRepository).save(any());
        verify(ledgerService).recordMovement(
                any(), any(), any(), any(), any(), any(), any()
        );
    }

    @Test
    void rejectsDisputeFromUnrelatedUser() {
        Pago payment = payment("FONDOS_RETENIDOS");
        when(paymentRepository.findByIdForUpdate(8)).thenReturn(Optional.of(payment));

        assertThrows(
                ForbiddenOperationException.class,
                () -> service.open(
                        99,
                        8,
                        new CreatePaymentDisputeRequest(
                                "Motivo",
                                "Descripcion suficientemente amplia.",
                                "PAGO_ESTUDIANTE"
                        )
                )
        );
        verify(disputeRepository, never()).save(any());
    }

    @Test
    void resolvesDisputeByReleasingSandboxPayout() {
        DisputaPago dispute = dispute();
        Pago payment = payment("EN_DISPUTA");
        TrabajoAsignado job = job();
        when(disputeRepository.findByIdForUpdate(3)).thenReturn(Optional.of(dispute));
        when(paymentRepository.findByIdForUpdate(8)).thenReturn(Optional.of(payment));
        when(jobRepository.findByIdForUpdate(5)).thenReturn(Optional.of(job));

        PaymentDisputeResponse response = service.resolve(
                1,
                3,
                new ResolvePaymentDisputeRequest(
                        "LIBERAR_ESTUDIANTE",
                        "La evidencia confirma que el trabajo fue completado."
                )
        );

        assertEquals("RESUELTA_ESTUDIANTE", response.estadoDisputa());
        assertEquals("PAGO_LIBERADO", payment.getEstadoPago());
        assertEquals("FINALIZADO", job.getEstadoTrabajo());
        verify(ledgerService).registerSandboxPayout(payment);
    }

    @Test
    void resolvesDisputeWithSandboxRefund() {
        DisputaPago dispute = dispute();
        Pago payment = payment("EN_DISPUTA");
        TrabajoAsignado job = job();
        when(disputeRepository.findByIdForUpdate(3)).thenReturn(Optional.of(dispute));
        when(paymentRepository.findByIdForUpdate(8)).thenReturn(Optional.of(payment));
        when(jobRepository.findByIdForUpdate(5)).thenReturn(Optional.of(job));

        service.resolve(
                1,
                3,
                new ResolvePaymentDisputeRequest(
                        "REEMBOLSAR_CLIENTE",
                        "La evidencia confirma el incumplimiento de la entrega."
                )
        );

        assertEquals("REEMBOLSADO", payment.getEstadoPago());
        assertEquals("CANCELADO_DISPUTA", job.getEstadoTrabajo());
        verify(ledgerService).registerSandboxRefund(payment, dispute, 1,
                "La evidencia confirma el incumplimiento de la entrega.");
    }

    @Test
    void doesNotResolveAnAlreadyClosedDispute() {
        DisputaPago dispute = dispute();
        dispute.setEstadoDisputa("RESUELTA_CLIENTE");
        when(disputeRepository.findByIdForUpdate(3)).thenReturn(Optional.of(dispute));

        assertThrows(
                ResourceConflictException.class,
                () -> service.resolve(
                        1,
                        3,
                        new ResolvePaymentDisputeRequest(
                                "REEMBOLSAR_CLIENTE",
                                "Esta resolucion ya no debe volver a ejecutarse."
                        )
                )
        );
        verify(paymentRepository, never()).findByIdForUpdate(any());
    }

    private Pago payment(String state) {
        Pago payment = new Pago();
        payment.setIdPago(8);
        payment.setIdTrabajo(5);
        payment.setIdCliente(10);
        payment.setIdEstudiante(20);
        payment.setUuidPago(UUID.randomUUID());
        payment.setProveedorPago("PAGADITO");
        payment.setEntornoPago("SANDBOX");
        payment.setMetodoPago("PAGADITO");
        payment.setMonedaCobro("NIO");
        payment.setMontoEstudiante(new BigDecimal("100.00"));
        payment.setMontoTotalCliente(new BigDecimal("101.00"));
        payment.setEstadoPago(state);
        payment.setFechaActualizacion(LocalDateTime.now());
        return payment;
    }

    private DisputaPago dispute() {
        DisputaPago dispute = new DisputaPago();
        dispute.setIdDisputa(3);
        dispute.setIdPago(8);
        dispute.setIdUsuarioAbre(10);
        dispute.setMotivo("Incumplimiento");
        dispute.setDescripcion("La entrega no coincide con el acuerdo.");
        dispute.setSolucionSolicitada("REEMBOLSO_CLIENTE");
        dispute.setMontoDisputado(new BigDecimal("101.00"));
        dispute.setEstadoDisputa("ABIERTA");
        dispute.setPrioridad("NORMAL");
        dispute.setFechaApertura(LocalDateTime.now());
        dispute.setFechaActualizacion(LocalDateTime.now());
        return dispute;
    }

    private TrabajoAsignado job() {
        TrabajoAsignado job = new TrabajoAsignado();
        job.setIdTrabajo(5);
        job.setIdTarea(4);
        job.setIdEstudiante(20);
        job.setEstadoTrabajo("EN_PROCESO");
        return job;
    }
}
