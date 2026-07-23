package com.t4kash.api.marketplace.service;

import com.t4kash.api.marketplace.dto.CreateTaskRequest;
import com.t4kash.api.marketplace.dto.TaskResponse;
import com.t4kash.api.marketplace.entity.Tarea;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
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
        when(categoriaRepository.existsById(1)).thenReturn(true);
        when(usuarioRepository.existsById(1)).thenReturn(true);
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
}
