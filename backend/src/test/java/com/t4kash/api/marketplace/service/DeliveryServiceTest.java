package com.t4kash.api.marketplace.service;

import com.t4kash.api.communication.service.NotificationService;
import com.t4kash.api.exception.ForbiddenOperationException;
import com.t4kash.api.finance.service.PaymentService;
import com.t4kash.api.marketplace.dto.CreateDeliveryRequest;
import com.t4kash.api.marketplace.dto.DeliveryResponse;
import com.t4kash.api.marketplace.entity.Entrega;
import com.t4kash.api.marketplace.entity.Tarea;
import com.t4kash.api.marketplace.entity.TrabajoAsignado;
import com.t4kash.api.marketplace.repository.CategoriaTareaRepository;
import com.t4kash.api.marketplace.repository.EntregaRepository;
import com.t4kash.api.marketplace.repository.PostulacionRepository;
import com.t4kash.api.marketplace.repository.TareaRepository;
import com.t4kash.api.marketplace.repository.TrabajoAsignadoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeliveryServiceTest {
    @Mock
    private CategoriaTareaRepository categoriaRepository;
    @Mock
    private TareaRepository tareaRepository;
    @Mock
    private PostulacionRepository postulacionRepository;
    @Mock
    private TrabajoAsignadoRepository trabajoRepository;
    @Mock
    private EntregaRepository entregaRepository;
    @Mock
    private NotificationService notificationService;

    private DeliveryService service;

    @BeforeEach
    void setUp() {
        TaskService taskService = new TaskService(
                categoriaRepository,
                tareaRepository,
                postulacionRepository,
                trabajoRepository
        );
        JobService jobService = new JobService(trabajoRepository, taskService);
        service = new DeliveryService(
                entregaRepository,
                trabajoRepository,
                taskService,
                jobService,
                notificationService
        );
    }

    @Test
    void creatingDeliveryRegistersItAsSent() {
        TrabajoAsignado job = job(50, "EN_PROCESO");
        when(trabajoRepository.findById(50)).thenReturn(Optional.of(job));
        when(tareaRepository.findById(10)).thenReturn(Optional.of(task(
                10,
                "ASIGNADA",
                LocalDateTime.now().plusDays(1)
        )));
        when(entregaRepository.save(any(Entrega.class))).thenAnswer(invocation -> {
            Entrega delivery = invocation.getArgument(0);
            delivery.setIdEntrega(200);
            return delivery;
        });

        DeliveryResponse response = service.createDelivery(
                1,
                50,
                new CreateDeliveryRequest("Entrega funcional del trabajo.")
        );

        assertEquals(200, response.idEntrega());
        assertEquals(50, response.idTrabajo());
        assertEquals("ENVIADA", response.estadoEntrega());
        assertEquals("Entrega funcional del trabajo.", response.descripcionEntrega());
    }

    @Test
    void approvingDeliveryFinalizesAssignedJob() {
        TrabajoAsignado job = job(50, "EN_PROCESO");
        Entrega delivery = delivery(200, 50, "ENVIADA");
        when(entregaRepository.findById(200)).thenReturn(Optional.of(delivery));
        when(trabajoRepository.findById(50)).thenReturn(Optional.of(job));
        when(tareaRepository.findById(10)).thenReturn(Optional.of(task(
                10,
                "ASIGNADA",
                LocalDateTime.now().plusDays(1)
        )));
        when(entregaRepository.save(delivery)).thenReturn(delivery);

        DeliveryResponse response = service.approveDelivery(1, 200);

        assertEquals("APROBADA", response.estadoEntrega());
        assertEquals("FINALIZADO", job.getEstadoTrabajo());
        verify(trabajoRepository).save(job);
    }

    @Test
    void approvingCashDeliveryWaitsForStudentConfirmation() {
        PaymentService paymentService = org.mockito.Mockito.mock(PaymentService.class);
        TaskService taskService = new TaskService(
                categoriaRepository,
                tareaRepository,
                postulacionRepository,
                trabajoRepository
        );
        DeliveryService cashService = new DeliveryService(
                entregaRepository,
                trabajoRepository,
                taskService,
                new JobService(trabajoRepository, taskService),
                notificationService,
                paymentService
        );
        TrabajoAsignado job = job(50, "EN_PROCESO");
        Entrega delivery = delivery(200, 50, "ENVIADA");
        when(entregaRepository.findById(200)).thenReturn(Optional.of(delivery));
        when(trabajoRepository.findById(50)).thenReturn(Optional.of(job));
        when(tareaRepository.findById(10)).thenReturn(Optional.of(task(
                10,
                "ASIGNADA",
                LocalDateTime.now().plusDays(1)
        )));
        when(entregaRepository.save(delivery)).thenReturn(delivery);
        when(paymentService.releaseForApprovedDelivery(job)).thenReturn(false);

        cashService.approveDelivery(1, 200);

        assertEquals(PaymentService.JOB_CASH_CONFIRMATION_PENDING, job.getEstadoTrabajo());
    }

    @Test
    void creatingDeliveryRejectsUsersWhoAreNotAssigned() {
        TrabajoAsignado job = job(50, "EN_PROCESO");
        when(trabajoRepository.findById(50)).thenReturn(Optional.of(job));

        assertThrows(
                ForbiddenOperationException.class,
                () -> service.createDelivery(
                        99,
                        50,
                        new CreateDeliveryRequest("Entrega ajena.")
                )
        );
    }

    private Tarea task(
            Integer id,
            String status,
            LocalDateTime applicationDeadline
    ) {
        Tarea task = new Tarea();
        task.setIdTarea(id);
        task.setTitulo("Oportunidad de prueba");
        task.setDescripcion("Descripcion completa para la oportunidad de prueba.");
        task.setPresupuesto(new BigDecimal("25.00"));
        task.setFechaPublicacion(LocalDateTime.now().minusHours(1));
        task.setFechaLimitePostulacion(applicationDeadline);
        task.setFechaLimite(applicationDeadline.plusDays(2));
        task.setEstadoTarea(status);
        task.setIdCategoria(1);
        task.setIdCliente(1);
        task.setTipoOportunidad("TAREA");
        task.setModalidad("REMOTA");
        task.setVisibilidad("PUBLICA");
        return task;
    }

    private TrabajoAsignado job(Integer id, String status) {
        TrabajoAsignado job = new TrabajoAsignado();
        job.setIdTrabajo(id);
        job.setIdTarea(10);
        job.setIdEstudiante(1);
        job.setFechaInicio(LocalDateTime.now().minusHours(1));
        job.setFechaEntregaEsperada(LocalDateTime.now().plusDays(2));
        job.setEstadoTrabajo(status);
        return job;
    }

    private Entrega delivery(Integer id, Integer jobId, String status) {
        Entrega delivery = new Entrega();
        delivery.setIdEntrega(id);
        delivery.setIdTrabajo(jobId);
        delivery.setDescripcionEntrega("Entrega funcional del trabajo.");
        delivery.setFechaEntrega(LocalDateTime.now());
        delivery.setEstadoEntrega(status);
        return delivery;
    }
}
