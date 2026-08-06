package com.t4kash.api.moderation.service;

import com.t4kash.api.exception.ForbiddenOperationException;
import com.t4kash.api.exception.ResourceConflictException;
import com.t4kash.api.marketplace.entity.Tarea;
import com.t4kash.api.marketplace.repository.TareaRepository;
import com.t4kash.api.marketplace.repository.UsuarioRepository;
import com.t4kash.api.marketplace.service.TaskService;
import com.t4kash.api.moderation.dto.CreateTaskReportRequest;
import com.t4kash.api.moderation.dto.ReportResponse;
import com.t4kash.api.moderation.dto.ReviewReportRequest;
import com.t4kash.api.moderation.entity.Reporte;
import com.t4kash.api.moderation.repository.ReporteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {
    @Mock
    private ReporteRepository reportRepository;
    @Mock
    private TareaRepository taskRepository;
    @Mock
    private UsuarioRepository userRepository;
    @Mock
    private TaskService taskService;
    @Mock
    private AuditService auditService;

    private ReportService service;

    @BeforeEach
    void setUp() {
        service = new ReportService(
                reportRepository,
                taskRepository,
                userRepository,
                taskService,
                auditService
        );
    }

    @Test
    void createsPendingTaskReport() {
        Tarea task = task(7, 20, "PUBLICADA");
        when(taskRepository.findById(7)).thenReturn(Optional.of(task));
        when(reportRepository.save(any(Reporte.class))).thenAnswer(invocation -> {
            Reporte saved = invocation.getArgument(0);
            saved.setIdReporte(3);
            return saved;
        });

        ReportResponse response = service.createTaskReport(
                10,
                7,
                new CreateTaskReportRequest(
                        "POSIBLE_ESTAFA",
                        "Solicita pagos fuera de la plataforma."
                )
        );

        assertEquals(3, response.idReporte());
        assertEquals("PENDIENTE", response.estadoReporte());
        assertEquals("POSIBLE_ESTAFA", response.categoriaReporte());
        assertEquals(20, response.idUsuarioReportado());
    }

    @Test
    void rejectsReportsAgainstOwnTask() {
        when(taskRepository.findById(7))
                .thenReturn(Optional.of(task(7, 10, "PUBLICADA")));

        ForbiddenOperationException error = assertThrows(
                ForbiddenOperationException.class,
                () -> service.createTaskReport(
                        10,
                        7,
                        new CreateTaskReportRequest("OTRO", null)
                )
        );

        assertEquals(
                "No puedes reportar una publicacion propia.",
                error.getMessage()
        );
    }

    @Test
    void preventsDuplicatePendingReport() {
        when(taskRepository.findById(7))
                .thenReturn(Optional.of(task(7, 20, "PUBLICADA")));
        when(reportRepository
                .existsByIdUsuarioReportaAndIdTareaAndEstadoReporte(
                        10,
                        7,
                        "PENDIENTE"
                ))
                .thenReturn(true);

        ResourceConflictException error = assertThrows(
                ResourceConflictException.class,
                () -> service.createTaskReport(
                        10,
                        7,
                        new CreateTaskReportRequest("OTRO", null)
                )
        );

        assertEquals(
                "Ya tienes un reporte pendiente para esta publicacion.",
                error.getMessage()
        );
    }

    @Test
    void reviewPersistsResolutionAndAudit() {
        Reporte report = report(5, 7);
        when(reportRepository.findById(5)).thenReturn(Optional.of(report));
        when(reportRepository.save(report)).thenReturn(report);
        when(taskRepository.findById(7))
                .thenReturn(Optional.of(task(7, 20, "PUBLICADA")));

        ReportResponse response = service.review(
                1,
                5,
                new ReviewReportRequest(
                        "DESCARTADO",
                        "No se encontro una infraccion.",
                        false
                ),
                "127.0.0.1",
                "JUnit"
        );

        assertEquals("DESCARTADO", response.estadoReporte());
        verify(auditService).record(
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any()
        );
    }

    private Tarea task(Integer taskId, Integer ownerId, String status) {
        Tarea task = new Tarea();
        task.setIdTarea(taskId);
        task.setIdCliente(ownerId);
        task.setTitulo("Oportunidad de prueba");
        task.setEstadoTarea(status);
        return task;
    }

    private Reporte report(Integer reportId, Integer taskId) {
        Reporte report = new Reporte();
        report.setIdReporte(reportId);
        report.setIdUsuarioReporta(10);
        report.setIdUsuarioReportado(20);
        report.setIdTarea(taskId);
        report.setMotivo("Otro motivo");
        report.setEstadoReporte("PENDIENTE");
        report.setTipoReporte("TAREA");
        report.setCategoriaReporte("OTRO");
        return report;
    }
}
