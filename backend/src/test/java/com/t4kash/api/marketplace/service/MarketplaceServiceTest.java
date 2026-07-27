package com.t4kash.api.marketplace.service;

import com.t4kash.api.exception.ResourceConflictException;
import com.t4kash.api.marketplace.dto.CreateApplicationRequest;
import com.t4kash.api.marketplace.dto.CreateTaskRequest;
import com.t4kash.api.marketplace.dto.JobResponse;
import com.t4kash.api.marketplace.dto.TaskResponse;
import com.t4kash.api.marketplace.entity.Postulacion;
import com.t4kash.api.marketplace.entity.Tarea;
import com.t4kash.api.marketplace.entity.TrabajoAsignado;
import com.t4kash.api.marketplace.repository.CategoriaTareaRepository;
import com.t4kash.api.marketplace.repository.EntregaRepository;
import com.t4kash.api.marketplace.repository.PostulacionRepository;
import com.t4kash.api.marketplace.repository.TareaRepository;
import com.t4kash.api.marketplace.repository.TrabajoAsignadoRepository;
import com.t4kash.api.marketplace.repository.UsuarioEstudianteRepository;
import com.t4kash.api.marketplace.repository.UsuarioRepository;
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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MarketplaceServiceTest {
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
    private UsuarioRepository usuarioRepository;
    @Mock
    private UsuarioEstudianteRepository estudianteRepository;

    private MarketplaceService service;

    @BeforeEach
    void setUp() {
        service = new MarketplaceService(
                categoriaRepository,
                tareaRepository,
                postulacionRepository,
                trabajoRepository,
                entregaRepository,
                usuarioRepository,
                estudianteRepository
        );
        lenient().when(categoriaRepository.existsById(1)).thenReturn(true);
        lenient().when(usuarioRepository.existsById(1)).thenReturn(true);
    }

    @Test
    void remoteTaskDiscardsCoordinates() {
        mockTaskSave();
        TaskResponse response = service.createTask(request(
                "REMOTA",
                "Referencia que no debe guardarse",
                new BigDecimal("12.114990"),
                new BigDecimal("-86.236170")
        ));

        assertEquals("REMOTA", response.modalidad());
        assertNull(response.direccionReferencia());
        assertNull(response.latitud());
        assertNull(response.longitud());
    }

    @Test
    void presencialTaskKeepsCoordinates() {
        mockTaskSave();
        TaskResponse response = service.createTask(request(
                "presencial",
                "Entrada principal del campus",
                new BigDecimal("12.114990"),
                new BigDecimal("-86.236170")
        ));

        assertEquals("PRESENCIAL", response.modalidad());
        assertEquals("Entrada principal del campus", response.direccionReferencia());
        assertEquals(new BigDecimal("12.114990"), response.latitud());
        assertEquals(new BigDecimal("-86.236170"), response.longitud());
    }

    @Test
    void presencialTaskRequiresBothCoordinates() {
        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> service.createTask(request(
                        "PRESENCIAL",
                        "Campus",
                        null,
                        null
                ))
        );

        assertEquals(
                "Las tareas presenciales o hibridas requieren latitud y longitud.",
                error.getMessage()
        );
    }

    @Test
    void listingTasksClosesExpiredPublications() {
        Tarea expiredTask = task(
                10,
                "PUBLICADA",
                LocalDateTime.now().minusMinutes(1)
        );
        when(tareaRepository.findAllByOrderByFechaPublicacionDesc())
                .thenReturn(List.of(expiredTask));

        List<TaskResponse> response = service.listTasks();

        assertEquals("CERRADA", expiredTask.getEstadoTarea());
        assertEquals("CERRADA", response.getFirst().estadoTarea());
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
                        10,
                        new CreateApplicationRequest(
                                1,
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

        JobResponse response = service.acceptApplication(100);

        assertEquals(50, response.idTrabajo());
        assertEquals("ASIGNADA", task.getEstadoTarea());
        assertEquals("ACEPTADA", accepted.getEstadoPostulacion());
        assertEquals("RECHAZADA", remaining.getEstadoPostulacion());
        verify(postulacionRepository).saveAll(List.of(remaining));
    }

    private CreateTaskRequest request(
            String modalidad,
            String direccion,
            BigDecimal latitud,
            BigDecimal longitud
    ) {
        return new CreateTaskRequest(
                "Diseñar una pantalla",
                "Crear una pantalla completa para una aplicación universitaria.",
                new BigDecimal("25.00"),
                null,
                null,
                1,
                1,
                "TAREA",
                modalidad,
                "PUBLICA",
                direccion,
                latitud,
                longitud
        );
    }

    private void mockTaskSave() {
        when(tareaRepository.save(any(Tarea.class))).thenAnswer(invocation -> {
            Tarea tarea = invocation.getArgument(0);
            tarea.setIdTarea(10);
            return tarea;
        });
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
        return application;
    }
}
