package com.t4kash.api.marketplace.controller;

import com.t4kash.api.identity.dto.AuthenticatedUserResponse;
import com.t4kash.api.identity.service.AuthenticatedUserService;
import com.t4kash.api.marketplace.dto.AttachmentResponse;
import com.t4kash.api.marketplace.service.AttachmentService;
import com.t4kash.api.marketplace.service.DownloadedAttachment;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api")
@Tag(name = "Archivos", description = "Adjuntos privados de tareas y entregas")
@SecurityRequirement(name = "bearerAuth")
public class AttachmentController {
    private final AttachmentService attachmentService;
    private final AuthenticatedUserService authenticatedUserService;

    public AttachmentController(
            AttachmentService attachmentService,
            AuthenticatedUserService authenticatedUserService
    ) {
        this.attachmentService = attachmentService;
        this.authenticatedUserService = authenticatedUserService;
    }

    @GetMapping("/tasks/{taskId}/attachments")
    @Operation(summary = "Listar archivos adjuntos de una tarea")
    public List<AttachmentResponse> listTaskAttachments(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false)
            String authorization,
            @PathVariable Integer taskId
    ) {
        authenticatedUserService.requireUser(authorization);
        return attachmentService.listTaskAttachments(taskId);
    }

    @PostMapping(
            value = "/tasks/{taskId}/attachments",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @Operation(summary = "Adjuntar archivo a una tarea")
    public AttachmentResponse attachToTask(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false)
            String authorization,
            @PathVariable Integer taskId,
            @RequestParam("file") MultipartFile file
    ) {
        AuthenticatedUserResponse user =
                authenticatedUserService.requireRole(authorization, "CLIENTE");
        return attachmentService.attachToTask(taskId, user.idUsuario(), file);
    }

    @GetMapping("/deliveries/{deliveryId}/attachments")
    @Operation(summary = "Listar archivos adjuntos de una entrega")
    public List<AttachmentResponse> listDeliveryAttachments(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false)
            String authorization,
            @PathVariable Integer deliveryId
    ) {
        AuthenticatedUserResponse user = authenticatedUserService.requireUser(authorization);
        return attachmentService.listDeliveryAttachments(deliveryId, user.idUsuario());
    }

    @GetMapping("/jobs/{jobId}/attachments")
    @Operation(summary = "Listar archivos de las entregas de un trabajo")
    public List<AttachmentResponse> listJobAttachments(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false)
            String authorization,
            @PathVariable Integer jobId
    ) {
        AuthenticatedUserResponse user = authenticatedUserService.requireUser(authorization);
        return attachmentService.listJobAttachments(jobId, user.idUsuario());
    }

    @PostMapping(
            value = "/deliveries/{deliveryId}/attachments",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @Operation(summary = "Adjuntar archivo a una entrega")
    public AttachmentResponse attachToDelivery(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false)
            String authorization,
            @PathVariable Integer deliveryId,
            @RequestParam("file") MultipartFile file
    ) {
        AuthenticatedUserResponse user =
                authenticatedUserService.requireRole(authorization, "ESTUDIANTE");
        return attachmentService.attachToDelivery(deliveryId, user.idUsuario(), file);
    }

    @GetMapping("/attachments/{attachmentId}/download")
    @Operation(summary = "Descargar un archivo adjunto privado")
    public ResponseEntity<byte[]> download(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false)
            String authorization,
            @PathVariable Integer attachmentId
    ) {
        AuthenticatedUserResponse user = authenticatedUserService.requireUser(authorization);
        DownloadedAttachment attachment = attachmentService.download(
                attachmentId,
                user.idUsuario(),
                user.roles().stream().anyMatch("ADMIN"::equalsIgnoreCase)
        );
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(attachment.fileName(), StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(attachment.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentLength(attachment.content().length)
                .body(attachment.content());
    }
}
