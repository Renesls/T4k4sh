package com.t4kash.api.admin.controller;

import com.t4kash.api.admin.dto.AdminSummaryResponse;
import com.t4kash.api.admin.service.AdminService;
import com.t4kash.api.identity.dto.AuthenticatedUserResponse;
import com.t4kash.api.identity.dto.PublicIdentityResponse;
import com.t4kash.api.identity.service.PublicProfileService;
import com.t4kash.api.identity.web.CurrentUser;
import com.t4kash.api.marketplace.dto.TaskResponse;
import com.t4kash.api.moderation.dto.ReportResponse;
import com.t4kash.api.moderation.dto.ReviewReportRequest;
import com.t4kash.api.moderation.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@Tag(name = "Administracion", description = "Moderacion y seguimiento operativo")
@SecurityRequirement(name = "bearerAuth")
public class AdminController {
    private final AdminService adminService;
    private final ReportService reportService;
    private final PublicProfileService profileService;

    public AdminController(
            AdminService adminService,
            ReportService reportService,
            PublicProfileService profileService
    ) {
        this.adminService = adminService;
        this.reportService = reportService;
        this.profileService = profileService;
    }

    @GetMapping("/summary")
    @Operation(summary = "Consultar resumen administrativo")
    public AdminSummaryResponse getSummary(@CurrentUser(role = "ADMIN") AuthenticatedUserResponse admin) {
        return adminService.getSummary();
    }

    @GetMapping("/tasks")
    @Operation(summary = "Listar publicaciones para moderacion")
    public List<TaskResponse> listTasks(
            @CurrentUser(role = "ADMIN") AuthenticatedUserResponse admin,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        List<TaskResponse> tasks = adminService.listTasks(page, size);
        Map<Integer, PublicIdentityResponse> identities = profileService.getIdentities(
                tasks.stream().map(TaskResponse::idCliente).toList()
        );
        return tasks.stream()
                .map(task -> task.withClient(identities.get(task.idCliente())))
                .toList();
    }

    @DeleteMapping("/tasks/{taskId}")
    @Operation(summary = "Retirar una publicacion como administrador")
    public TaskResponse cancelTask(
            @CurrentUser(role = "ADMIN") AuthenticatedUserResponse admin,
            @PathVariable Integer taskId,
            HttpServletRequest servletRequest
    ) {
        TaskResponse task = adminService.cancelTask(
                admin.idUsuario(),
                taskId,
                clientIp(servletRequest),
                servletRequest.getHeader(HttpHeaders.USER_AGENT)
        );
        return task.withClient(profileService.getIdentity(task.idCliente()));
    }

    @GetMapping("/reports")
    @Operation(summary = "Listar reportes de moderacion")
    public List<ReportResponse> listReports(
            @CurrentUser(role = "ADMIN") AuthenticatedUserResponse admin,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        return reportService.listAll(page, size);
    }

    @PostMapping("/reports/{reportId}/review")
    @Operation(summary = "Resolver o descartar un reporte")
    public ReportResponse reviewReport(
            @CurrentUser(role = "ADMIN") AuthenticatedUserResponse admin,
            @PathVariable Integer reportId,
            @Valid @RequestBody ReviewReportRequest request,
            HttpServletRequest servletRequest
    ) {
        return reportService.review(
                admin.idUsuario(),
                reportId,
                request,
                clientIp(servletRequest),
                servletRequest.getHeader(HttpHeaders.USER_AGENT)
        );
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
