package com.t4kash.api.marketplace.controller;

import com.t4kash.api.identity.dto.AuthenticatedUserResponse;
import com.t4kash.api.identity.service.AuthenticatedUserService;
import com.t4kash.api.marketplace.dto.ApplicationResponse;
import com.t4kash.api.marketplace.dto.CategoriaResponse;
import com.t4kash.api.marketplace.dto.CreateApplicationRequest;
import com.t4kash.api.marketplace.dto.CreateDeliveryRequest;
import com.t4kash.api.marketplace.dto.CreateTaskRequest;
import com.t4kash.api.marketplace.dto.DeliveryResponse;
import com.t4kash.api.marketplace.dto.JobResponse;
import com.t4kash.api.marketplace.dto.TaskResponse;
import com.t4kash.api.marketplace.service.MarketplaceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
@Tag(name = "Marketplace", description = "Oportunidades, postulaciones, trabajos asignados y entregas")
public class MarketplaceController {
    private final MarketplaceService marketplaceService;
    private final AuthenticatedUserService authenticatedUserService;

    public MarketplaceController(
            MarketplaceService marketplaceService,
            AuthenticatedUserService authenticatedUserService
    ) {
        this.marketplaceService = marketplaceService;
        this.authenticatedUserService = authenticatedUserService;
    }

    @GetMapping("/categories")
    @Operation(summary = "Listar categorias activas")
    public List<CategoriaResponse> listCategories() {
        return marketplaceService.listCategories();
    }

    @GetMapping("/tasks")
    @Operation(summary = "Listar oportunidades")
    public List<TaskResponse> listTasks() {
        return marketplaceService.listTasks();
    }

    @PostMapping("/tasks")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crear oportunidad")
    @SecurityRequirement(name = "bearerAuth")
    public TaskResponse createTask(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false)
            String authorization,
            @Valid @RequestBody CreateTaskRequest request
    ) {
        AuthenticatedUserResponse user =
                authenticatedUserService.requireRole(authorization, "CLIENTE");
        return marketplaceService.createTask(user.idUsuario(), request);
    }

    @GetMapping("/tasks/{idTarea}")
    @Operation(summary = "Obtener detalle de una oportunidad")
    public TaskResponse getTask(@PathVariable Integer idTarea) {
        return marketplaceService.getTask(idTarea);
    }

    @GetMapping("/tasks/{idTarea}/applications")
    @Operation(summary = "Listar postulaciones de una oportunidad")
    @SecurityRequirement(name = "bearerAuth")
    public List<ApplicationResponse> listApplications(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false)
            String authorization,
            @PathVariable Integer idTarea
    ) {
        AuthenticatedUserResponse user =
                authenticatedUserService.requireRole(authorization, "CLIENTE");
        return marketplaceService.listApplications(user.idUsuario(), idTarea);
    }

    @PostMapping("/tasks/{idTarea}/applications")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Postularse a una oportunidad")
    @SecurityRequirement(name = "bearerAuth")
    public ApplicationResponse applyToTask(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false)
            String authorization,
            @PathVariable Integer idTarea,
            @Valid @RequestBody CreateApplicationRequest request
    ) {
        AuthenticatedUserResponse user =
                authenticatedUserService.requireRole(authorization, "ESTUDIANTE");
        return marketplaceService.applyToTask(user.idUsuario(), idTarea, request);
    }

    @PostMapping("/applications/{idPostulacion}/accept")
    @Operation(summary = "Aceptar postulacion y crear trabajo asignado")
    @SecurityRequirement(name = "bearerAuth")
    public JobResponse acceptApplication(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false)
            String authorization,
            @PathVariable Integer idPostulacion
    ) {
        AuthenticatedUserResponse user =
                authenticatedUserService.requireRole(authorization, "CLIENTE");
        return marketplaceService.acceptApplication(user.idUsuario(), idPostulacion);
    }

    @PostMapping("/applications/{idPostulacion}/reject")
    @Operation(summary = "Rechazar postulacion")
    @SecurityRequirement(name = "bearerAuth")
    public ApplicationResponse rejectApplication(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false)
            String authorization,
            @PathVariable Integer idPostulacion
    ) {
        AuthenticatedUserResponse user =
                authenticatedUserService.requireRole(authorization, "CLIENTE");
        return marketplaceService.rejectApplication(user.idUsuario(), idPostulacion);
    }

    @GetMapping("/jobs")
    @Operation(summary = "Listar trabajos asignados")
    @SecurityRequirement(name = "bearerAuth")
    public List<JobResponse> listJobs(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false)
            String authorization
    ) {
        AuthenticatedUserResponse user = authenticatedUserService.requireUser(authorization);
        return marketplaceService.listJobs(user.idUsuario());
    }

    @GetMapping("/jobs/{idTrabajo}/deliveries")
    @Operation(summary = "Listar entregas de un trabajo")
    @SecurityRequirement(name = "bearerAuth")
    public List<DeliveryResponse> listDeliveries(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false)
            String authorization,
            @PathVariable Integer idTrabajo
    ) {
        AuthenticatedUserResponse user = authenticatedUserService.requireUser(authorization);
        return marketplaceService.listDeliveries(user.idUsuario(), idTrabajo);
    }

    @PostMapping("/jobs/{idTrabajo}/deliveries")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Enviar entrega de trabajo")
    @SecurityRequirement(name = "bearerAuth")
    public DeliveryResponse createDelivery(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false)
            String authorization,
            @PathVariable Integer idTrabajo,
            @Valid @RequestBody CreateDeliveryRequest request
    ) {
        AuthenticatedUserResponse user =
                authenticatedUserService.requireRole(authorization, "ESTUDIANTE");
        return marketplaceService.createDelivery(user.idUsuario(), idTrabajo, request);
    }

    @PostMapping("/deliveries/{idEntrega}/approve")
    @Operation(summary = "Aprobar entrega")
    @SecurityRequirement(name = "bearerAuth")
    public DeliveryResponse approveDelivery(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false)
            String authorization,
            @PathVariable Integer idEntrega
    ) {
        AuthenticatedUserResponse user =
                authenticatedUserService.requireRole(authorization, "CLIENTE");
        return marketplaceService.approveDelivery(user.idUsuario(), idEntrega);
    }
}
