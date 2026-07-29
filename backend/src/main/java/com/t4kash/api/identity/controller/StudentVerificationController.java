package com.t4kash.api.identity.controller;

import com.t4kash.api.identity.dto.AuthenticatedUserResponse;
import com.t4kash.api.identity.dto.ReviewStudentVerificationRequest;
import com.t4kash.api.identity.dto.StudentVerificationResponse;
import com.t4kash.api.identity.entity.VerificacionUsuario;
import com.t4kash.api.identity.service.AuthenticatedUserService;
import com.t4kash.api.identity.service.StudentVerificationService;
import com.t4kash.api.marketplace.dto.AttachmentResponse;
import com.t4kash.api.marketplace.service.AttachmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/student-verifications")
@Tag(name = "Verificacion estudiantil")
@SecurityRequirement(name = "bearerAuth")
public class StudentVerificationController {
    private final AuthenticatedUserService authenticatedUserService;
    private final StudentVerificationService verificationService;
    private final AttachmentService attachmentService;

    public StudentVerificationController(
            AuthenticatedUserService authenticatedUserService,
            StudentVerificationService verificationService,
            AttachmentService attachmentService
    ) {
        this.authenticatedUserService = authenticatedUserService;
        this.verificationService = verificationService;
        this.attachmentService = attachmentService;
    }

    @GetMapping("/me")
    @Operation(summary = "Consultar mi validacion estudiantil")
    public StudentVerificationResponse getMine(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false)
            String authorization
    ) {
        return verificationService.getCurrent(
                authenticatedUserService.requireUser(authorization).idUsuario()
        );
    }

    @PostMapping(value = "/me/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Enviar carnet o constancia universitaria")
    public AttachmentResponse uploadProof(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false)
            String authorization,
            @RequestParam("file") MultipartFile file
    ) {
        AuthenticatedUserResponse user =
                authenticatedUserService.requireRole(authorization, "CLIENTE");
        VerificacionUsuario verification =
                verificationService.requireOrCreate(user.idUsuario());
        AttachmentResponse response = attachmentService.attachToVerification(
                verification.getIdVerificacion(),
                user.idUsuario(),
                file
        );
        verificationService.markPendingReview(
                user.idUsuario(),
                verification.getIdVerificacion()
        );
        return response;
    }

    @GetMapping("/pending")
    @Operation(summary = "Listar validaciones pendientes")
    public List<StudentVerificationResponse> listPending(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false)
            String authorization
    ) {
        authenticatedUserService.requireRole(authorization, "ADMIN");
        return verificationService.listPending();
    }

    @PostMapping("/{userId}/approve")
    @Operation(summary = "Aprobar perfil estudiantil")
    public StudentVerificationResponse approve(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false)
            String authorization,
            @PathVariable Integer userId,
            @Valid @RequestBody ReviewStudentVerificationRequest request
    ) {
        authenticatedUserService.requireRole(authorization, "ADMIN");
        return verificationService.approve(userId, request.observacion());
    }

    @PostMapping("/{userId}/reject")
    @Operation(summary = "Rechazar perfil estudiantil")
    public StudentVerificationResponse reject(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false)
            String authorization,
            @PathVariable Integer userId,
            @Valid @RequestBody ReviewStudentVerificationRequest request
    ) {
        authenticatedUserService.requireRole(authorization, "ADMIN");
        return verificationService.reject(userId, request.observacion());
    }
}
