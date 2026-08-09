package com.t4kash.api.marketplace.service;

import com.t4kash.api.marketplace.dto.CreateTaskRequest;
import com.t4kash.api.marketplace.dto.QuickTaskResponse;
import com.t4kash.api.marketplace.dto.TaskResponse;
import com.t4kash.api.marketplace.entity.Tarea;
import com.t4kash.api.marketplace.repository.CategoriaTareaRepository;
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
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {
    @Mock
    private CategoriaTareaRepository categoriaRepository;
    @Mock
    private TareaRepository tareaRepository;
    @Mock
    private PostulacionRepository postulacionRepository;
    @Mock
    private TrabajoAsignadoRepository trabajoRepository;

    private TaskService service;

    @BeforeEach
    void setUp() {
        service = new TaskService(
                categoriaRepository,
                tareaRepository,
                postulacionRepository,
                trabajoRepository
        );
        lenient().when(
                categoriaRepository.existsByIdCategoriaAndEstadoTrue(1)
        ).thenReturn(true);
    }

    @Test
    void remoteTaskDiscardsCoordinates() {
        mockTaskSave();
        TaskResponse response = service.createTask(1, request(
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
        TaskResponse response = service.createTask(1, request(
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
                () -> service.createTask(1, request(
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
    void nearbyQuickTasksAreFilteredAndSortedByDistance() {
        Tarea near = quickTask(10, 2, "12.115500", "-86.236170");
        Tarea far = quickTask(11, 3, "12.130000", "-86.236170");
        Tarea own = quickTask(12, 1, "12.115100", "-86.236170");
        when(
                tareaRepository
                        .findQuickTasksWithinBounds(
                                eq("RAPIDA"),
                                eq("PUBLICADA"),
                                any(BigDecimal.class),
                                any(BigDecimal.class),
                                any(BigDecimal.class),
                                any(BigDecimal.class)
                        )
        ).thenReturn(List.of(far, own, near));

        List<QuickTaskResponse> response = service.listNearbyQuickTasks(
                1,
                12.114990,
                -86.236170,
                1.0
        );

        assertEquals(1, response.size());
        assertEquals(10, response.getFirst().tarea().idTarea());
    }

    @Test
    void quickTaskMustBePresencial() {
        CreateTaskRequest request = new CreateTaskRequest(
                "Imprimir una pagina",
                "Necesito una impresion cerca del campus universitario.",
                new BigDecimal("30.00"),
                LocalDateTime.now().plusHours(1),
                LocalDateTime.now().plusHours(3),
                1,
                "RAPIDA",
                "REMOTA",
                "PUBLICA",
                null,
                null,
                null
        );

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> service.createTask(1, request)
        );

        assertEquals("Las tareas rapidas deben ser presenciales.", error.getMessage());
    }

    @Test
    void quickTaskCannotExceedOneThousandCordobas() {
        CreateTaskRequest request = new CreateTaskRequest(
                "Imprimir material urgente",
                "Necesito imprimir material cerca del campus universitario.",
                new BigDecimal("1000.01"),
                LocalDateTime.now().plusHours(1),
                LocalDateTime.now().plusHours(3),
                1,
                "RAPIDA",
                "PRESENCIAL",
                "PUBLICA",
                "Entrada principal",
                new BigDecimal("12.114990"),
                new BigDecimal("-86.236170")
        );

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> service.createTask(1, request)
        );

        assertEquals(
                "El pago de una tarea rapida no puede superar C$1,000.",
                error.getMessage()
        );
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

    private Tarea quickTask(
            Integer id,
            Integer clientId,
            String latitude,
            String longitude
    ) {
        Tarea task = task(id, "PUBLICADA", LocalDateTime.now().plusHours(2));
        task.setIdCliente(clientId);
        task.setTipoOportunidad("RAPIDA");
        task.setModalidad("PRESENCIAL");
        task.setLatitud(new BigDecimal(latitude));
        task.setLongitud(new BigDecimal(longitude));
        return task;
    }
}
