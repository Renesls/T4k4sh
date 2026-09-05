package com.t4kash.api.finance.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.t4kash.api.communication.service.NotificationService;
import com.t4kash.api.finance.dto.PaymentResponse;
import com.t4kash.api.finance.entity.Pago;
import com.t4kash.api.finance.repository.EventoWebhookPagoRepository;
import com.t4kash.api.finance.repository.PagoRepository;
import com.t4kash.api.finance.repository.TransaccionPagoRepository;
import com.t4kash.api.marketplace.entity.Postulacion;
import com.t4kash.api.marketplace.entity.Tarea;
import com.t4kash.api.marketplace.entity.TrabajoAsignado;
import com.t4kash.api.marketplace.repository.TrabajoAsignadoRepository;
import com.t4kash.api.marketplace.service.TaskService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PaymentServiceTest {
    private PagoRepository paymentRepository;
    private TransaccionPagoRepository movementRepository;
    private TrabajoAsignadoRepository jobRepository;
    private PaymentService service;

    @BeforeEach
    void setUp() {
        paymentRepository = mock(PagoRepository.class);
        movementRepository = mock(TransaccionPagoRepository.class);
        jobRepository = mock(TrabajoAsignadoRepository.class);
        service = new PaymentService(
                paymentRepository,
                movementRepository,
                mock(EventoWebhookPagoRepository.class),
                jobRepository,
                mock(TaskService.class),
                mock(PagaditoClient.class),
                mock(PagaditoWebhookVerifier.class),
                mock(NotificationService.class),
                new ObjectMapper(),
                new BigDecimal("10.00"),
                new BigDecimal("5.00"),
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
    void createsProtectedPaymentSplittingTheTakeRateBetweenBothSides() {
        TrabajoAsignado job = job(30);
        Tarea task = task("REMOTA");
        Postulacion application = application(new BigDecimal("100.00"));

        PaymentResponse response = service.createForAcceptedApplication(
                job,
                task,
                application,
                "PAGADITO"
        );

        // Precio acordado C$ 100.00: el cliente paga 10 % encima y al estudiante se le retiene 5 %.
        assertEquals(new BigDecimal("95.00"), response.montoEstudiante());
        assertEquals(new BigDecimal("15.00"), response.comisionPlataforma());
        assertEquals(new BigDecimal("110.00"), response.montoTotalCliente());
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
        payment.setMonedaCobro("NIO");
        payment.setMontoEstudiante(new BigDecimal("100.00"));
        payment.setEstadoPago("FONDOS_RETENIDOS");
        payment.setFechaActualizacion(LocalDateTime.now());
        when(paymentRepository.findByIdTrabajo(30)).thenReturn(Optional.of(payment));

        assertTrue(service.releaseForApprovedDelivery(job));

        assertEquals("PAGO_LIBERADO", payment.getEstadoPago());
        verify(movementRepository).save(any());
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
        when(paymentRepository.findByIdTrabajo(30)).thenReturn(Optional.of(payment));
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
