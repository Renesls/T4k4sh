package com.t4kash.api.marketplace.service;

import com.t4kash.api.communication.service.ConversationService;
import com.t4kash.api.communication.service.NotificationService;
import com.t4kash.api.exception.ForbiddenOperationException;
import com.t4kash.api.exception.ResourceConflictException;
import com.t4kash.api.marketplace.dto.CreateApplicationRequest;
import com.t4kash.api.marketplace.dto.JobResponse;
import com.t4kash.api.marketplace.entity.Postulacion;
import com.t4kash.api.marketplace.entity.Tarea;
import com.t4kash.api.marketplace.entity.TrabajoAsignado;
import com.t4kash.api.marketplace.entity.UsuarioEstudiante;
import com.t4kash.api.marketplace.repository.CategoriaTareaRepository;
import com.t4kash.api.marketplace.repository.PostulacionRepository;
import com.t4kash.api.marketplace.repository.TareaRepository;
import com.t4kash.api.marketplace.repository.TrabajoAsignadoRepository;
import com.t4kash.api.marketplace.repository.UsuarioEstudianteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApplicationServiceTest {
    @Mock
    private CategoriaTareaRepository categoriaRepository;
    @Mock
    private TareaRepository tareaRepository;
    @Mock
    private PostulacionRepository postulacionRepository;
    @Mock
    private TrabajoAsignadoRepository trabajoRepository;
    @Mock
    private UsuarioEstudianteRepository estudianteRepository;
    @Mock
    private ConversationService conversationService;
    @Mock
    private NotificationService notificationService;

    private ApplicationService service;

    @BeforeEach
    void setUp() {
        TaskService taskService = new TaskService(
                categoriaRepository,
                tareaRepository,
                postulacionRepository,
                trabajoRepository
        );
        service = new ApplicationService(
                postulacionRepository,
                trabajoRepository,
                estudianteRepository,
                taskService,
                conversationService,
                notificationService
        );
    }

    @Test
    void expiredTaskRejectsNewApplications() {
        Tarea expiredTask = task(
                10,
                "PUBLICADA",
                LocalDateTime.now().minusMinutes(1)
        );
        when(tareaRepository.findById(10)).thenReturn(Optional.of(expiredTask));

        ResourceConflictException error = assertThrows(
                ResourceConflictException.class,
                () -> service.applyToTask(
                        1,
                        10,
                        new CreateApplicationRequest(
                                "Quiero participar.",
                                new BigDecimal("20.00")
                        )
                )
        );

        assertEquals(
                "El plazo de postulacion para esta tarea ya finalizo.",
                error.getMessage()
        );
        assertEquals("CERRADA", expiredTask.getEstadoTarea());
    }

    @Test
    void acceptingApplicationRejectsRemainingPendingApplications() {
        Tarea task = task(
                10,
                "PUBLICADA",
                LocalDateTime.now().plusDays(1)
        );
        Postulacion accepted = application(100, 10, 1);
        Postulacion remaining = application(101, 10, 2);

        when(postulacionRepository.findById(100)).thenReturn(Optional.of(accepted));
        when(trabajoRepository.findByIdTarea(10)).thenReturn(Optional.empty());
        when(tareaRepository.findById(10)).thenReturn(Optional.of(task));
        when(estudianteRepository.findByIdForUpdate(1))
                .thenReturn(Optional.of(new UsuarioEstudiante()));
        when(trabajoRepository.countByIdEstudianteAndEstadoTrabajo(1, "EN_PROCESO"))
                .thenReturn(0L);
        when(
                postulacionRepository
                        .findByIdTareaAndEstadoPostulacionAndIdPostulacionNot(
                                10,
                                "PENDIENTE",
                                100
                        )
        ).thenReturn(List.of(remaining));
        when(trabajoRepository.save(any(TrabajoAsignado.class)))
                .thenAnswer(invocation -> {
                    TrabajoAsignado job = invocation.getArgument(0);
                    job.setIdTrabajo(50);
                    return job;
                });

        JobResponse response = service.acceptApplication(1, 100);

        assertEquals(50, response.idTrabajo());
        assertEquals("ASIGNADA", task.getEstadoTarea());
        assertEquals("ACEPTADA", accepted.getEstadoPostulacion());
        assertEquals("RECHAZADA", remaining.getEstadoPostulacion());
        verify(postulacionRepository).saveAll(List.of(remaining));
    }

    @Test
    void rejectedStudentCanApplyAgainWithSecondAttempt() {
        Tarea task = task(10, "PUBLICADA", LocalDateTime.now().plusDays(1));
        Postulacion previous = application(90, 10, 2);
        previous.setEstadoPostulacion("RECHAZADA");
        previous.setNumeroIntento(1);
        when(tareaRepository.findById(10)).thenReturn(Optional.of(task));
        when(estudianteRepository.existsById(2)).thenReturn(true);
        when(trabajoRepository.countByIdEstudianteAndEstadoTrabajo(2, "EN_PROCESO"))
                .thenReturn(0L);
        when(
                postulacionRepository
                        .findFirstByIdTareaAndIdEstudianteOrderByNumeroIntentoDesc(10, 2)
        ).thenReturn(Optional.of(previous));
        when(postulacionRepository.save(any(Postulacion.class)))
                .thenAnswer(invocation -> {
                    Postulacion application = invocation.getArgument(0);
                    application.setIdPostulacion(100);
                    return application;
                });

        var response = service.applyToTask(
                2,
                10,
                new CreateApplicationRequest(
                        "Nueva propuesta con cambios.",
                        new BigDecimal("18.00")
                )
        );

        assertEquals(2, response.numeroIntento());
        assertEquals("PENDIENTE", response.estadoPostulacion());
    }

    @Test
    void studentCannotExceedThreeApplicationAttempts() {
        Tarea task = task(10, "PUBLICADA", LocalDateTime.now().plusDays(1));
        Postulacion previous = application(90, 10, 2);
        previous.setEstadoPostulacion("RECHAZADA");
        previous.setNumeroIntento(3);
        when(tareaRepository.findById(10)).thenReturn(Optional.of(task));
        when(estudianteRepository.existsById(2)).thenReturn(true);
        when(trabajoRepository.countByIdEstudianteAndEstadoTrabajo(2, "EN_PROCESO"))
                .thenReturn(0L);
        when(
                postulacionRepository
                        .findFirstByIdTareaAndIdEstudianteOrderByNumeroIntentoDesc(10, 2)
        ).thenReturn(Optional.of(previous));

        assertThrows(
                ResourceConflictException.class,
                () -> service.applyToTask(
                        2,
                        10,
                        new CreateApplicationRequest(
                                "Cuarta propuesta.",
                                new BigDecimal("18.00")
                        )
                )
        );
    }

    @Test
    void secondActiveJobCancelsOtherPendingApplications() {
        Tarea task = task(10, "PUBLICADA", LocalDateTime.now().plusDays(1));
        Postulacion accepted = application(100, 10, 2);
        accepted.setNumeroIntento(1);
        Postulacion otherPending = application(110, 20, 2);
        otherPending.setNumeroIntento(1);
        when(postulacionRepository.findById(100)).thenReturn(Optional.of(accepted));
        when(tareaRepository.findById(10)).thenReturn(Optional.of(task));
        when(trabajoRepository.findByIdTarea(10)).thenReturn(Optional.empty());
        when(estudianteRepository.findByIdForUpdate(2))
                .thenReturn(Optional.of(new UsuarioEstudiante()));
        when(trabajoRepository.countByIdEstudianteAndEstadoTrabajo(2, "EN_PROCESO"))
                .thenReturn(1L);
        when(
                postulacionRepository.findByIdEstudianteAndEstadoPostulacion(
                        2,
                        "PENDIENTE"
                )
        ).thenReturn(List.of(otherPending));
        when(trabajoRepository.save(any(TrabajoAsignado.class)))
                .thenAnswer(invocation -> {
                    TrabajoAsignado job = invocation.getArgument(0);
                    job.setIdTrabajo(50);
                    return job;
                });

        service.acceptApplication(1, 100);

        assertEquals("CANCELADA_LIMITE", otherPending.getEstadoPostulacion());
    }

    @Test
    void acceptingApplicationRejectsUsersWhoDoNotOwnTheTask() {
        Postulacion application = application(100, 10, 2);
        when(postulacionRepository.findById(100)).thenReturn(Optional.of(application));
        when(tareaRepository.findById(10)).thenReturn(Optional.of(task(
                10,
                "PUBLICADA",
                LocalDateTime.now().plusDays(1)
        )));

        ForbiddenOperationException error = assertThrows(
                ForbiddenOperationException.class,
                () -> service.acceptApplication(99, 100)
        );

        assertEquals(
                "Solo el propietario de la tarea puede realizar esta accion.",
                error.getMessage()
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

    private Postulacion application(
            Integer id,
            Integer taskId,
            Integer studentId
    ) {
        Postulacion application = new Postulacion();
        application.setIdPostulacion(id);
        application.setIdTarea(taskId);
        application.setIdEstudiante(studentId);
        application.setMensaje("Propuesta de prueba.");
        application.setPrecioPropuesto(new BigDecimal("20.00"));
        application.setFechaPostulacion(LocalDateTime.now().minusMinutes(10));
        application.setEstadoPostulacion("PENDIENTE");
        application.setNumeroIntento(1);
        return application;
    }
}
