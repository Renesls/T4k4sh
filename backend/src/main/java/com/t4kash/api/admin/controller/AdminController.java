package com.t4kash.api.admin.controller;

import com.t4kash.api.admin.dto.AdminSummaryResponse;
import com.t4kash.api.admin.service.AdminService;
import com.t4kash.api.identity.service.AuthenticatedUserService;
import com.t4kash.api.marketplace.dto.TaskResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@Tag(name = "Administracion", description = "Moderacion y seguimiento operativo")
@SecurityRequirement(name = "bearerAuth")
public class AdminController {
    private final AuthenticatedUserService authenticatedUserService;
    private final AdminService adminService;

    public AdminController(
            AuthenticatedUserService authenticatedUserService,
            AdminService adminService
    ) {
        this.authenticatedUserService = authenticatedUserService;
        this.adminService = adminService;
    }

    @GetMapping("/summary")
    @Operation(summary = "Consultar resumen administrativo")
    public AdminSummaryResponse getSummary(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false)
            String authorization
    ) {
        authenticatedUserService.requireRole(authorization, "ADMIN");
        return adminService.getSummary();
    }

    @GetMapping("/tasks")
    @Operation(summary = "Listar publicaciones para moderacion")
    public List<TaskResponse> listTasks(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false)
            String authorization
    ) {
        authenticatedUserService.requireRole(authorization, "ADMIN");
        return adminService.listTasks();
    }

    @DeleteMapping("/tasks/{taskId}")
    @Operation(summary = "Retirar una publicacion como administrador")
    public TaskResponse cancelTask(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false)
            String authorization,
            @PathVariable Integer taskId
    ) {
        authenticatedUserService.requireRole(authorization, "ADMIN");
        return adminService.cancelTask(taskId);
    }
}
