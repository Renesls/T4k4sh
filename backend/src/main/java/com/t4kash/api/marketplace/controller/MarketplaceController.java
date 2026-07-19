package com.t4kash.api.marketplace.controller;

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

import java.util.List;

@RestController
@RequestMapping("/api")
@Tag(name = "Marketplace", description = "Oportunidades, postulaciones, trabajos asignados y entregas")
public class MarketplaceController {
    private final MarketplaceService marketplaceService;

    public MarketplaceController(MarketplaceService marketplaceService) {
        this.marketplaceService = marketplaceService;
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
    public TaskResponse createTask(@Valid @RequestBody CreateTaskRequest request) {
        return marketplaceService.createTask(request);
    }

    @GetMapping("/tasks/{idTarea}")
    @Operation(summary = "Obtener detalle de una oportunidad")
    public TaskResponse getTask(@PathVariable Integer idTarea) {
        return marketplaceService.getTask(idTarea);
    }

    @GetMapping("/tasks/{idTarea}/applications")
    @Operation(summary = "Listar postulaciones de una oportunidad")
    public List<ApplicationResponse> listApplications(@PathVariable Integer idTarea) {
        return marketplaceService.listApplications(idTarea);
    }

    @PostMapping("/tasks/{idTarea}/applications")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Postularse a una oportunidad")
    public ApplicationResponse applyToTask(
            @PathVariable Integer idTarea,
            @Valid @RequestBody CreateApplicationRequest request
    ) {
        return marketplaceService.applyToTask(idTarea, request);
    }

    @PostMapping("/applications/{idPostulacion}/accept")
    @Operation(summary = "Aceptar postulacion y crear trabajo asignado")
    public JobResponse acceptApplication(@PathVariable Integer idPostulacion) {
        return marketplaceService.acceptApplication(idPostulacion);
    }

    @PostMapping("/applications/{idPostulacion}/reject")
    @Operation(summary = "Rechazar postulacion")
    public ApplicationResponse rejectApplication(@PathVariable Integer idPostulacion) {
        return marketplaceService.rejectApplication(idPostulacion);
    }

    @GetMapping("/jobs")
    @Operation(summary = "Listar trabajos asignados")
    public List<JobResponse> listJobs() {
        return marketplaceService.listJobs();
    }

    @GetMapping("/jobs/{idTrabajo}/deliveries")
    @Operation(summary = "Listar entregas de un trabajo")
    public List<DeliveryResponse> listDeliveries(@PathVariable Integer idTrabajo) {
        return marketplaceService.listDeliveries(idTrabajo);
    }

    @PostMapping("/jobs/{idTrabajo}/deliveries")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Enviar entrega de trabajo")
    public DeliveryResponse createDelivery(
            @PathVariable Integer idTrabajo,
            @Valid @RequestBody CreateDeliveryRequest request
    ) {
        return marketplaceService.createDelivery(idTrabajo, request);
    }

    @PostMapping("/deliveries/{idEntrega}/approve")
    @Operation(summary = "Aprobar entrega")
    public DeliveryResponse approveDelivery(@PathVariable Integer idEntrega) {
        return marketplaceService.approveDelivery(idEntrega);
    }
}
