package com.t4kash.api.admin.service;

import com.t4kash.api.admin.dto.AdminSummaryResponse;
import com.t4kash.api.identity.service.StudentVerificationService;
import com.t4kash.api.exception.ResourceNotFoundException;
import com.t4kash.api.marketplace.dto.TaskResponse;
import com.t4kash.api.marketplace.entity.Tarea;
import com.t4kash.api.marketplace.repository.TareaRepository;
import com.t4kash.api.marketplace.repository.TrabajoAsignadoRepository;
import com.t4kash.api.marketplace.repository.UsuarioRepository;
import com.t4kash.api.marketplace.service.TaskService;
import com.t4kash.api.moderation.service.AuditService;
import com.t4kash.api.moderation.service.ReportService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
public class AdminService {
    private final UsuarioRepository usuarioRepository;
    private final TareaRepository tareaRepository;
    private final TrabajoAsignadoRepository trabajoRepository;
    private final StudentVerificationService verificationService;
    private final TaskService taskService;
    private final ReportService reportService;
    private final AuditService auditService;

    public AdminService(
            UsuarioRepository usuarioRepository,
            TareaRepository tareaRepository,
            TrabajoAsignadoRepository trabajoRepository,
            StudentVerificationService verificationService,
            TaskService taskService,
            ReportService reportService,
            AuditService auditService
    ) {
        this.usuarioRepository = usuarioRepository;
        this.tareaRepository = tareaRepository;
        this.trabajoRepository = trabajoRepository;
        this.verificationService = verificationService;
        this.taskService = taskService;
        this.reportService = reportService;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public AdminSummaryResponse getSummary() {
        return new AdminSummaryResponse(
                usuarioRepository.count(),
                verificationService.countPending(),
                reportService.countPending(),
                tareaRepository.countByEstadoTareaIgnoreCase("PUBLICADA"),
                trabajoRepository.count()
        );
    }

    @Transactional
    public List<TaskResponse> listTasks() {
        return taskService.listTasksForAdmin();
    }

    @Transactional
    public TaskResponse cancelTask(
            Integer adminUserId,
            Integer taskId,
            String ipAddress,
            String userAgent
    ) {
        Tarea task = tareaRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "La publicacion indicada no existe."
                ));
        String previousStatus = task.getEstadoTarea();
        TaskResponse cancelled = taskService.cancelTaskAsAdmin(taskId);
        auditService.record(
                adminUserId,
                "RETIRAR_PUBLICACION",
                "tareas",
                taskId,
                Map.of("estadoTarea", previousStatus),
                Map.of("estadoTarea", cancelled.estadoTarea()),
                ipAddress,
                userAgent
        );
        return cancelled;
    }
}
