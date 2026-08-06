package com.t4kash.api.marketplace.controller;

import com.t4kash.api.identity.dto.AuthenticatedUserResponse;
import com.t4kash.api.identity.web.CurrentUser;
import com.t4kash.api.marketplace.dto.ApplicationResponse;
import com.t4kash.api.marketplace.dto.CategoriaResponse;
import com.t4kash.api.marketplace.dto.CreateApplicationRequest;
import com.t4kash.api.marketplace.dto.CreateDeliveryRequest;
import com.t4kash.api.marketplace.dto.CreateTaskRequest;
import com.t4kash.api.marketplace.dto.DeliveryResponse;
import com.t4kash.api.marketplace.dto.JobResponse;
import com.t4kash.api.marketplace.dto.TaskResponse;
import com.t4kash.api.marketplace.service.ApplicationService;
import com.t4kash.api.marketplace.service.DeliveryService;
import com.t4kash.api.marketplace.service.JobService;
import com.t4kash.api.marketplace.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
@Tag(name = "Marketplace", description = "Oportunidades, postulaciones, trabajos asignados y entregas")
public class MarketplaceController {
    private final TaskService taskService;
    private final ApplicationService applicationService;
    private final JobService jobService;
    private final DeliveryService deliveryService;

    public MarketplaceController(
            TaskService taskService,
            ApplicationService applicationService,
            JobService jobService,
            DeliveryService deliveryService
    ) {
        this.taskService = taskService;
        this.applicationService = applicationService;
        this.jobService = jobService;
        this.deliveryService = deliveryService;
    }

    @GetMapping("/categories")
    @Operation(summary = "Listar categorias activas")
    public List<CategoriaResponse> listCategories() {
        return taskService.listCategories();
    }

    @GetMapping("/tasks")
    @Operation(summary = "Listar oportunidades")
    public List<TaskResponse> listTasks() {
        return taskService.listTasks();
    }

    @PostMapping("/tasks")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crear oportunidad")
    @SecurityRequirement(name = "bearerAuth")
    public TaskResponse createTask(
            @CurrentUser(role = "CLIENTE") AuthenticatedUserResponse user,
            @Valid @RequestBody CreateTaskRequest request
    ) {
        return taskService.createTask(user.idUsuario(), request);
    }

    @PutMapping("/tasks/{idTarea}")
    @Operation(summary = "Editar una oportunidad activa")
    @SecurityRequirement(name = "bearerAuth")
    public TaskResponse updateTask(
            @CurrentUser(role = "CLIENTE") AuthenticatedUserResponse user,
            @PathVariable Integer idTarea,
            @Valid @RequestBody CreateTaskRequest request
    ) {
        return taskService.updateTask(user.idUsuario(), idTarea, request);
    }

    @DeleteMapping("/tasks/{idTarea}")
    @Operation(summary = "Cancelar una oportunidad activa")
    @SecurityRequirement(name = "bearerAuth")
    public TaskResponse cancelTask(
            @CurrentUser(role = "CLIENTE") AuthenticatedUserResponse user,
            @PathVariable Integer idTarea
    ) {
        return taskService.cancelTask(user.idUsuario(), idTarea);
    }

    @GetMapping("/tasks/{idTarea}")
    @Operation(summary = "Obtener detalle de una oportunidad")
    public TaskResponse getTask(@PathVariable Integer idTarea) {
        return taskService.getTask(idTarea);
    }

    @GetMapping("/tasks/{idTarea}/applications")
    @Operation(summary = "Listar postulaciones de una oportunidad")
    @SecurityRequirement(name = "bearerAuth")
    public List<ApplicationResponse> listApplications(
            @CurrentUser(role = "CLIENTE") AuthenticatedUserResponse user,
            @PathVariable Integer idTarea
    ) {
        return applicationService.listApplications(user.idUsuario(), idTarea);
    }

    @PostMapping("/tasks/{idTarea}/applications")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Postularse a una oportunidad")
    @SecurityRequirement(name = "bearerAuth")
    public ApplicationResponse applyToTask(
            @CurrentUser(role = "ESTUDIANTE") AuthenticatedUserResponse user,
            @PathVariable Integer idTarea,
            @Valid @RequestBody CreateApplicationRequest request
    ) {
        return applicationService.applyToTask(user.idUsuario(), idTarea, request);
    }

    @GetMapping("/applications/me")
    @Operation(summary = "Listar mis postulaciones")
    @SecurityRequirement(name = "bearerAuth")
    public List<ApplicationResponse> listMyApplications(
            @CurrentUser(role = "ESTUDIANTE") AuthenticatedUserResponse user
    ) {
        return applicationService.listMyApplications(user.idUsuario());
    }

    @PostMapping("/applications/{idPostulacion}/accept")
    @Operation(summary = "Aceptar postulacion y crear trabajo asignado")
    @SecurityRequirement(name = "bearerAuth")
    public JobResponse acceptApplication(
            @CurrentUser(role = "CLIENTE") AuthenticatedUserResponse user,
            @PathVariable Integer idPostulacion
    ) {
        return applicationService.acceptApplication(user.idUsuario(), idPostulacion);
    }

    @PostMapping("/applications/{idPostulacion}/reject")
    @Operation(summary = "Rechazar postulacion")
    @SecurityRequirement(name = "bearerAuth")
    public ApplicationResponse rejectApplication(
            @CurrentUser(role = "CLIENTE") AuthenticatedUserResponse user,
            @PathVariable Integer idPostulacion
    ) {
        return applicationService.rejectApplication(user.idUsuario(), idPostulacion);
    }

    @GetMapping("/jobs")
    @Operation(summary = "Listar trabajos asignados")
    @SecurityRequirement(name = "bearerAuth")
    public List<JobResponse> listJobs(@CurrentUser AuthenticatedUserResponse user) {
        return jobService.listJobs(user.idUsuario());
    }

    @GetMapping("/jobs/{idTrabajo}/deliveries")
    @Operation(summary = "Listar entregas de un trabajo")
    @SecurityRequirement(name = "bearerAuth")
    public List<DeliveryResponse> listDeliveries(
            @CurrentUser AuthenticatedUserResponse user,
            @PathVariable Integer idTrabajo
    ) {
        return deliveryService.listDeliveries(user.idUsuario(), idTrabajo);
    }

    @PostMapping("/jobs/{idTrabajo}/deliveries")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Enviar entrega de trabajo")
    @SecurityRequirement(name = "bearerAuth")
    public DeliveryResponse createDelivery(
            @CurrentUser(role = "ESTUDIANTE") AuthenticatedUserResponse user,
            @PathVariable Integer idTrabajo,
            @Valid @RequestBody CreateDeliveryRequest request
    ) {
        return deliveryService.createDelivery(user.idUsuario(), idTrabajo, request);
    }

    @PostMapping("/deliveries/{idEntrega}/approve")
    @Operation(summary = "Aprobar entrega")
    @SecurityRequirement(name = "bearerAuth")
    public DeliveryResponse approveDelivery(
            @CurrentUser(role = "CLIENTE") AuthenticatedUserResponse user,
            @PathVariable Integer idEntrega
    ) {
        return deliveryService.approveDelivery(user.idUsuario(), idEntrega);
    }
}
