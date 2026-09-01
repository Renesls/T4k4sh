package com.t4kash.api.moderation.controller;

import com.t4kash.api.identity.dto.AuthenticatedUserResponse;
import com.t4kash.api.identity.web.CurrentUser;
import com.t4kash.api.moderation.dto.CreateTaskReportRequest;
import com.t4kash.api.moderation.dto.ReportResponse;
import com.t4kash.api.moderation.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@RestController
@RequestMapping("/api")
@Tag(name = "Reportes", description = "Reportes de moderacion del marketplace")
@SecurityRequirement(name = "bearerAuth")
public class ReportController {
    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @PostMapping("/tasks/{taskId}/reports")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Reportar una publicacion")
    public ReportResponse createTaskReport(
            @CurrentUser AuthenticatedUserResponse user,
            @PathVariable Integer taskId,
            @Valid @RequestBody CreateTaskReportRequest request
    ) {
        return reportService.createTaskReport(user.idUsuario(), taskId, request);
    }

    @GetMapping("/reports/me")
    @Operation(summary = "Consultar mis reportes")
    public List<ReportResponse> listMine(
            @CurrentUser AuthenticatedUserResponse user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        return reportService.listMine(user.idUsuario(), page, size);
    }
}
