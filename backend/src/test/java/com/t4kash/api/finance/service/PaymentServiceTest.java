package com.t4kash.api.finance.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.t4kash.api.communication.service.NotificationService;
import com.t4kash.api.finance.dto.PaymentResponse;
import com.t4kash.api.finance.entity.EventoWebhookPago;
import com.t4kash.api.finance.entity.Pago;
import com.t4kash.api.finance.repository.EventoWebhookPagoRepository;
import com.t4kash.api.finance.repository.DisputaPagoRepository;
import com.t4kash.api.finance.repository.PagoRepository;
import com.t4kash.api.finance.repository.TransaccionPagoRepository;
import com.t4kash.api.marketplace.entity.Postulacion;
import com.t4kash.api.marketplace.entity.Tarea;
import com.t4kash.api.marketplace.entity.TrabajoAsignado;
import com.t4kash.api.marketplace.repository.TrabajoAsignadoRepository;
import com.t4kash.api.marketplace.service.TaskService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PaymentServiceTest {
    private PagoRepository paymentRepository;
    private TransaccionPagoRepository movementRepository;
    private EventoWebhookPagoRepository webhookRepository;
    private DisputaPagoRepository disputeRepository;
    private FinancialLedgerService ledgerService;
    private TrabajoAsignadoRepository jobRepository;
    private PagaditoClient pagaditoClient;
    private PagaditoWebhookVerifier webhookVerifier;
    private PaymentService service;

    @BeforeEach
    void setUp() {
        paymentRepository = mock(PagoRepository.class);
        movementRepository = mock(TransaccionPagoRepository.class);
        webhookRepository = mock(EventoWebhookPagoRepository.class);
        disputeRepository = mock(DisputaPagoRepository.class);
        ledgerService = mock(FinancialLedgerService.class);
        jobRepository = mock(TrabajoAsignadoRepository.class);
        pagaditoClient = mock(PagaditoClient.class);
        webhookVerifier = mock(PagaditoWebhookVerifier.class);
        service = new PaymentService(
                paymentRepository,
                movementRepository,
                disputeRepository,
                ledgerService,
                webhookRepository,
                jobRepository,
                mock(TaskService.class),
                pagaditoClient,
                webhookVerifier,
                mock(NotificationService.class),
                new ObjectMapper(),
                new BigDecimal("1.00"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                "SANDBOX",
                "https://t4k4sh.onrender.com"
        );
        when(paymentRepository.save(any(Pago.class))).thenAnswer(invocation -> {
            Pago payment = invocation.getArgument(0);
            payment.setIdPago(9);
            return payment;
        });
    }

    @Test
    void createsProtectedPaymentWithTransparentOnePercentFee() {
        TrabajoAsignado job = job(30);
        Tarea task = task("REMOTA");
        Postulacion application = application(new BigDecimal("100.00"));

        PaymentResponse response = service.createForAcceptedApplication(
                job,
                task,
                application,
                "PAGADITO"
        );

        assertEquals(new BigDecimal("100.00"), response.montoEstudiante());
        assertEquals(new BigDecimal("1.00"), response.comisionPlataforma());
        assertEquals(new BigDecimal("101.00"), response.montoTotalCliente());
        assertEquals("PENDIENTE_PAGO", response.estadoPago());
        verify(movementRepository).save(any());
    }

    @Test
    void rejectsCashForNonPresentialTask() {
        assertThrows(
                IllegalArgumentException.class,
                () -> service.createForAcceptedApplication(
                        job(30),
                        task("HIBRIDA"),
                        application(new BigDecimal("100.00")),
                        "EFECTIVO"
                )
        );
    }

    @Test
    void releasesHeldFundsWhenDeliveryIsApproved() {
        TrabajoAsignado job = job(30);
        Pago payment = new Pago();
        payment.setIdPago(9);
        payment.setIdTrabajo(30);
        payment.setIdCliente(1);
        payment.setIdEstudiante(2);
        payment.setUuidPago(UUID.randomUUID());
        payment.setMetodoPago("PAGADITO");
        payment.setProveedorPago("PAGADITO");
        payment.setEntornoPago("SANDBOX");
        payment.setMonedaCobro("NIO");
        payment.setMontoEstudiante(new BigDecimal("100.00"));
        payment.setEstadoPago("FONDOS_RETENIDOS");
        payment.setFechaActualizacion(LocalDateTime.now());
        when(paymentRepository.findByIdTrabajoForUpdate(30)).thenReturn(Optional.of(payment));

        assertTrue(service.releaseForApprovedDelivery(job));

        assertEquals("PAGO_LIBERADO", payment.getEstadoPago());
        verify(movementRepository).save(any());
        verify(ledgerService).registerSandboxPayout(payment);
        verify(paymentRepository).save(payment);
    }

    @Test
    void cashPaymentNeedsStudentConfirmationAfterClientApproval() {
        TrabajoAsignado job = job(30);
        Pago payment = new Pago();
        payment.setIdPago(9);
        payment.setIdTrabajo(30);
        payment.setIdCliente(1);
        payment.setIdEstudiante(2);
        payment.setUuidPago(UUID.randomUUID());
        payment.setMetodoPago("EFECTIVO");
        payment.setProveedorPago("EXTERNO");
        payment.setEntornoPago("EXTERNO");
        payment.setMonedaCobro("NIO");
        payment.setMontoEstudiante(new BigDecimal("100.00"));
        payment.setPorcentajeComisionPlataforma(BigDecimal.ZERO);
        payment.setComisionPlataforma(BigDecimal.ZERO);
        payment.setComisionProcesador(BigDecimal.ZERO);
        payment.setImpuestoProcesador(BigDecimal.ZERO);
        payment.setMontoTotalCliente(new BigDecimal("100.00"));
        payment.setEstadoPago("PAGO_EXTERNO_PENDIENTE");
        payment.setFechaCreacion(LocalDateTime.now());
        payment.setFechaActualizacion(LocalDateTime.now());
        when(paymentRepository.findByIdTrabajoForUpdate(30)).thenReturn(Optional.of(payment));
        when(jobRepository.findById(30)).thenReturn(Optional.of(job));

        assertFalse(service.releaseForApprovedDelivery(job));
        assertEquals("PAGO_EXTERNO_PENDIENTE", payment.getEstadoPago());

        job.setEstadoTrabajo(PaymentService.JOB_CASH_CONFIRMATION_PENDING);
        PaymentResponse response = service.confirmCashReceipt(2, 30);

        assertEquals("PAGO_EXTERNO_CONFIRMADO", response.estadoPago());
        assertEquals("FINALIZADO", job.getEstadoTrabajo());
        verify(jobRepository).save(job);
        verify(movementRepository).save(any());
    }

    @Test
    void lateRegisteredStatusDoesNotRegressHeldFunds() {
        Pago payment = completePayment("FONDOS_RETENIDOS");
        when(paymentRepository.findByIdForUpdate(9)).thenReturn(Optional.of(payment));
        when(pagaditoClient.getStatus("token-1"))
                .thenReturn(new PagaditoClient.TransactionStatus("REGISTERED", null, null));

        PaymentResponse response = service.refreshStatus(1, 9);

        assertEquals("FONDOS_RETENIDOS", response.estadoPago());
    }

    @Test
    void returnAssociatesTokenUsingCommerceReference() {
        Pago payment = completePayment("PENDIENTE_PAGO");
        payment.setReferenciaProveedor(null);
        when(paymentRepository.findByReferenciaComercioForUpdate("T4KASH-TEST"))
                .thenReturn(Optional.of(payment));
        when(pagaditoClient.getStatus("token-return"))
                .thenReturn(new PagaditoClient.TransactionStatus("REGISTERED", null, null));

        String status = service.processReturn("token-return", "T4KASH-TEST");

        assertEquals("PAGO_REGISTRADO", status);
        assertEquals("token-return", payment.getReferenciaProveedor());
    }

    @Test
    void webhookWithDifferentAmountRemainsAuditedAsError() {
        Pago payment = completePayment("PENDIENTE_PAGO");
        when(webhookRepository.findByProveedorPagoAndEntornoPagoAndIdEventoProveedor(
                "PAGADITO", "SANDBOX", "event-1"
        )).thenReturn(Optional.empty());
        when(paymentRepository.findByReferenciaComercioForUpdate("T4KASH-TEST"))
                .thenReturn(Optional.of(payment));
        when(webhookVerifier.verify(any(), any(), any())).thenReturn(true);
        String body = """
                {
                  "id": "event-1",
                  "event_type": "TRANSACTION.STATUS.CHANGE",
                  "resource": {
                    "ern": "T4KASH-TEST",
                    "token": "token-1",
                    "status": "COMPLETED",
                    "amount": {"total": "999.00", "currency": "NIO"}
                  }
                }
                """;

        assertThrows(
                IllegalArgumentException.class,
                () -> service.processWebhook(body, mock(PagaditoWebhookVerifier.Headers.class))
        );

        ArgumentCaptor<EventoWebhookPago> eventCaptor =
                ArgumentCaptor.forClass(EventoWebhookPago.class);
        verify(webhookRepository, atLeast(2)).save(eventCaptor.capture());
        assertEquals("ERROR", eventCaptor.getValue().getEstadoProcesamiento());
    }

    private Pago completePayment(String status) {
        Pago payment = new Pago();
        payment.setIdPago(9);
        payment.setIdTrabajo(30);
        payment.setIdCliente(1);
        payment.setIdEstudiante(2);
        payment.setUuidPago(UUID.randomUUID());
        payment.setProveedorPago("PAGADITO");
        payment.setEntornoPago("SANDBOX");
        payment.setMetodoPago("PAGADITO");
        payment.setMonedaCobro("NIO");
        payment.setMontoEstudiante(new BigDecimal("100.00"));
        payment.setPorcentajeComisionPlataforma(new BigDecimal("1.00"));
        payment.setComisionPlataforma(new BigDecimal("1.00"));
        payment.setComisionProcesador(BigDecimal.ZERO);
        payment.setImpuestoProcesador(BigDecimal.ZERO);
        payment.setMontoTotalCliente(new BigDecimal("101.00"));
        payment.setEstadoPago(status);
        payment.setReferenciaComercio("T4KASH-TEST");
        payment.setReferenciaProveedor("token-1");
        payment.setFechaCreacion(LocalDateTime.now());
        payment.setFechaActualizacion(LocalDateTime.now());
        return payment;
    }

    private TrabajoAsignado job(int id) {
        TrabajoAsignado job = new TrabajoAsignado();
        job.setIdTrabajo(id);
        job.setIdTarea(10);
        job.setIdEstudiante(2);
        job.setEstadoTrabajo("PENDIENTE_PAGO");
        job.setFechaInicio(LocalDateTime.now());
        return job;
    }

    private Tarea task(String modality) {
        Tarea task = new Tarea();
        task.setIdTarea(10);
        task.setIdCliente(1);
        task.setTitulo("Trabajo de prueba");
        task.setModalidad(modality);
        task.setPresupuesto(new BigDecimal("120.00"));
        return task;
    }

    private Postulacion application(BigDecimal price) {
        Postulacion application = new Postulacion();
        application.setIdEstudiante(2);
        application.setPrecioPropuesto(price);
        return application;
    }
}
