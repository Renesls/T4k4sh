package com.t4kash.api.moderation.service;

import com.t4kash.api.config.PaginationSupport;
import com.t4kash.api.exception.ForbiddenOperationException;
import com.t4kash.api.exception.ResourceConflictException;
import com.t4kash.api.exception.ResourceNotFoundException;
import com.t4kash.api.marketplace.dto.TaskResponse;
import com.t4kash.api.marketplace.entity.Tarea;
import com.t4kash.api.marketplace.entity.Usuario;
import com.t4kash.api.marketplace.repository.TareaRepository;
import com.t4kash.api.marketplace.repository.UsuarioRepository;
import com.t4kash.api.marketplace.service.TaskService;
import com.t4kash.api.moderation.dto.CreateTaskReportRequest;
import com.t4kash.api.moderation.dto.ReportResponse;
import com.t4kash.api.moderation.dto.ReviewReportRequest;
import com.t4kash.api.moderation.entity.Reporte;
import com.t4kash.api.moderation.repository.ReporteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ReportService {
    private static final String REPORT_TYPE_TASK = "TAREA";
    private static final String REPORT_PENDING = "PENDIENTE";
    private static final String REPORT_RESOLVED = "RESUELTO";
    private static final String REPORT_DISMISSED = "DESCARTADO";
    private static final String TASK_CANCELLED = "CANCELADA";
    private static final Set<String> VALID_REVIEW_STATES =
            Set.of(REPORT_RESOLVED, REPORT_DISMISSED);
    private static final Map<String, String> CATEGORY_LABELS = Map.of(
            "CONTENIDO_INAPROPIADO", "Contenido inapropiado",
            "POSIBLE_ESTAFA", "Posible estafa",
            "INFORMACION_FALSA", "Informacion falsa",
            "PUBLICACION_DUPLICADA", "Publicacion duplicada",
            "OTRO", "Otro motivo"
    );

    private final ReporteRepository reportRepository;
    private final TareaRepository taskRepository;
    private final UsuarioRepository userRepository;
    private final TaskService taskService;
    private final AuditService auditService;

    public ReportService(
            ReporteRepository reportRepository,
            TareaRepository taskRepository,
            UsuarioRepository userRepository,
            TaskService taskService,
            AuditService auditService
    ) {
        this.reportRepository = reportRepository;
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
        this.taskService = taskService;
        this.auditService = auditService;
    }

    @Transactional
    public ReportResponse createTaskReport(
            Integer currentUserId,
            Integer taskId,
            CreateTaskReportRequest request
    ) {
        Tarea task = requireTask(taskId);
        if (task.getIdCliente().equals(currentUserId)) {
            throw new ForbiddenOperationException(
                    "No puedes reportar una publicacion propia."
            );
        }
        if (TASK_CANCELLED.equalsIgnoreCase(task.getEstadoTarea())) {
            throw new ResourceConflictException(
                    "La publicacion ya fue retirada del marketplace."
            );
        }
        if (reportRepository.existsByIdUsuarioReportaAndIdTareaAndEstadoReporte(
                currentUserId,
                taskId,
                REPORT_PENDING
        )) {
            throw new ResourceConflictException(
                    "Ya tienes un reporte pendiente para esta publicacion."
            );
        }

        String category = normalizeCategory(request.categoriaReporte());
        Reporte report = new Reporte();
        report.setIdUsuarioReporta(currentUserId);
        report.setIdUsuarioReportado(task.getIdCliente());
        report.setIdTarea(taskId);
        report.setMotivo(CATEGORY_LABELS.get(category));
        report.setDescripcion(clean(request.descripcion()));
        report.setEstadoReporte(REPORT_PENDING);
        report.setFechaReporte(LocalDateTime.now());
        report.setTipoReporte(REPORT_TYPE_TASK);
        report.setCategoriaReporte(category);
        return toResponse(reportRepository.save(report));
    }

    @Transactional(readOnly = true)
    public List<ReportResponse> listMine(Integer currentUserId) {
        return listMine(currentUserId, 0, PaginationSupport.DEFAULT_SIZE);
    }

    @Transactional(readOnly = true)
    public List<ReportResponse> listMine(
            Integer currentUserId,
            int page,
            int size
    ) {
        return toResponses(reportRepository
                .findByIdUsuarioReportaOrderByFechaReporteDesc(
                        currentUserId,
                        PaginationSupport.page(page, size)
                ));
    }

    @Transactional(readOnly = true)
    public List<ReportResponse> listAll() {
        return listAll(0, PaginationSupport.DEFAULT_SIZE);
    }

    @Transactional(readOnly = true)
    public List<ReportResponse> listAll(int page, int size) {
        return toResponses(reportRepository.findAllByOrderByFechaReporteDesc(
                PaginationSupport.page(page, size)
        ));
    }

    @Transactional(readOnly = true)
    public long countPending() {
        return reportRepository.countByEstadoReporte(REPORT_PENDING);
    }

    @Transactional
    public ReportResponse review(
            Integer adminUserId,
            Integer reportId,
            ReviewReportRequest request,
            String ipAddress,
            String userAgent
    ) {
        Reporte report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "El reporte indicado no existe."
                ));
        if (!REPORT_PENDING.equals(report.getEstadoReporte())) {
            throw new ResourceConflictException(
                    "El reporte ya fue revisado."
            );
        }

        String newStatus = request.estadoReporte()
                .trim()
                .toUpperCase(Locale.ROOT);
        if (!VALID_REVIEW_STATES.contains(newStatus)) {
            throw new IllegalArgumentException(
                    "El estado debe ser RESUELTO o DESCARTADO."
            );
        }
        if (request.retirarPublicacion() && !REPORT_RESOLVED.equals(newStatus)) {
            throw new IllegalArgumentException(
                    "Solo un reporte resuelto puede retirar una publicacion."
            );
        }

        Map<String, Object> before = reportAuditData(
                report,
                null,
                false
        );
        TaskResponse cancelledTask = null;
        String previousTaskStatus = null;
        if (request.retirarPublicacion()) {
            if (report.getIdTarea() == null) {
                throw new ResourceConflictException(
                        "El reporte no esta relacionado con una publicacion."
                );
            }
            Tarea task = requireTask(report.getIdTarea());
            previousTaskStatus = task.getEstadoTarea();
            if (!TASK_CANCELLED.equalsIgnoreCase(previousTaskStatus)) {
                cancelledTask = taskService.cancelTaskAsAdmin(
                        report.getIdTarea()
                );
            }
        }

        report.setEstadoReporte(newStatus);
        Reporte saved = reportRepository.save(report);
        auditService.record(
                adminUserId,
                "REVISAR_REPORTE",
                "reportes",
                saved.getIdReporte(),
                before,
                reportAuditData(saved, clean(request.observacion()), request.retirarPublicacion()),
                ipAddress,
                userAgent
        );
        if (cancelledTask != null) {
            auditService.record(
                    adminUserId,
                    "RETIRAR_PUBLICACION_REPORTADA",
                    "tareas",
                    cancelledTask.idTarea(),
                    Map.of("estadoTarea", previousTaskStatus),
                    Map.of(
                            "estadoTarea", cancelledTask.estadoTarea(),
                            "idReporte", saved.getIdReporte()
                    ),
                    ipAddress,
                    userAgent
            );
        }
        return toResponse(saved);
    }

    private Map<String, Object> reportAuditData(
            Reporte report,
            String observation,
            boolean publicationRemoved
    ) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("estadoReporte", report.getEstadoReporte());
        data.put("idTarea", report.getIdTarea());
        data.put("categoriaReporte", report.getCategoriaReporte());
        data.put("observacion", observation);
        data.put("publicacionRetirada", publicationRemoved);
        return data;
    }

    private ReportResponse toResponse(Reporte report) {
        String reporterEmail = userEmail(report.getIdUsuarioReporta());
        String reportedEmail = userEmail(report.getIdUsuarioReportado());
        String taskTitle = report.getIdTarea() == null
                ? null
                : taskRepository.findById(report.getIdTarea())
                .map(Tarea::getTitulo)
                .orElse(null);
        return ReportResponse.fromEntity(
                report,
                reporterEmail,
                reportedEmail,
                taskTitle
        );
    }

    private List<ReportResponse> toResponses(List<Reporte> reports) {
        if (reports.isEmpty()) {
            return List.of();
        }
        Set<Integer> userIds = new HashSet<>();
        Set<Integer> taskIds = new HashSet<>();
        reports.forEach(report -> {
            if (report.getIdUsuarioReporta() != null) {
                userIds.add(report.getIdUsuarioReporta());
            }
            if (report.getIdUsuarioReportado() != null) {
                userIds.add(report.getIdUsuarioReportado());
            }
            if (report.getIdTarea() != null) {
                taskIds.add(report.getIdTarea());
            }
        });
        Map<Integer, Usuario> users = mapById(
                userRepository.findAllById(userIds),
                Usuario::getIdUsuario
        );
        Map<Integer, Tarea> tasks = mapById(
                taskRepository.findAllById(taskIds),
                Tarea::getIdTarea
        );
        return reports.stream()
                .map(report -> ReportResponse.fromEntity(
                        report,
                        email(users.get(report.getIdUsuarioReporta())),
                        email(users.get(report.getIdUsuarioReportado())),
                        report.getIdTarea() == null
                                ? null
                                : title(tasks.get(report.getIdTarea()))
                ))
                .toList();
    }

    private <T> Map<Integer, T> mapById(
            Iterable<T> items,
            Function<T, Integer> idExtractor
    ) {
        Map<Integer, T> result = new java.util.HashMap<>();
        items.forEach(item -> result.put(idExtractor.apply(item), item));
        return result;
    }

    private String email(Usuario user) {
        return user == null ? null : user.getCorreo();
    }

    private String title(Tarea task) {
        return task == null ? null : task.getTitulo();
    }

    private String userEmail(Integer userId) {
        if (userId == null) {
            return null;
        }
        return userRepository.findById(userId)
                .map(Usuario::getCorreo)
                .orElse(null);
    }

    private Tarea requireTask(Integer taskId) {
        return taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "La publicacion indicada no existe."
                ));
    }

    private String normalizeCategory(String value) {
        String category = value == null
                ? ""
                : value.trim().toUpperCase(Locale.ROOT);
        if (!CATEGORY_LABELS.containsKey(category)) {
            throw new IllegalArgumentException(
                    "Selecciona una categoria de reporte valida."
            );
        }
        return category;
    }

    private String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
