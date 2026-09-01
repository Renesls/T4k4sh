package com.t4kash.api.identity.controller;

import com.t4kash.api.identity.dto.AuthenticatedUserResponse;
import com.t4kash.api.identity.dto.IdentityVerificationSessionResponse;
import com.t4kash.api.identity.dto.IdentityVerificationStatusResponse;
import com.t4kash.api.identity.dto.IdentityWebhookResponse;
import com.t4kash.api.identity.service.DiditWebhookVerifier;
import com.t4kash.api.identity.service.IdentityVerificationService;
import com.t4kash.api.identity.web.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/identity-verifications")
@Tag(name = "Verificacion de identidad", description = "KYC alojado mediante Didit")
public class IdentityVerificationController {
    private final IdentityVerificationService verificationService;

    public IdentityVerificationController(
            IdentityVerificationService verificationService
    ) {
        this.verificationService = verificationService;
    }

    @GetMapping("/me")
    @Operation(summary = "Consultar mi estado de verificacion de identidad")
    @SecurityRequirement(name = "bearerAuth")
    public IdentityVerificationStatusResponse getMine(
            @CurrentUser AuthenticatedUserResponse user
    ) {
        return verificationService.getCurrent(user.idUsuario());
    }

    @PostMapping("/me/session")
    @Operation(summary = "Crear o recuperar una sesion alojada de Didit")
    @SecurityRequirement(name = "bearerAuth")
    public IdentityVerificationSessionResponse start(
            @CurrentUser AuthenticatedUserResponse user,
            @RequestParam(defaultValue = "PERFIL") String origen
    ) {
        return verificationService.start(user.idUsuario(), origen);
    }

    @PostMapping("/me/refresh")
    @Operation(summary = "Consultar nuevamente la decision autoritativa de Didit")
    @SecurityRequirement(name = "bearerAuth")
    public IdentityVerificationStatusResponse refresh(
            @CurrentUser AuthenticatedUserResponse user
    ) {
        return verificationService.refresh(user.idUsuario());
    }

    @PostMapping("/webhook")
    @Operation(summary = "Recibir un evento firmado de Didit")
    public IdentityWebhookResponse webhook(
            @RequestBody String rawBody,
            @RequestHeader(name = "X-Signature-V2", required = false) String signatureV2,
            @RequestHeader(name = "X-Signature", required = false) String signature,
            @RequestHeader(name = "X-Signature-Simple", required = false) String signatureSimple,
            @RequestHeader(name = "X-Timestamp", required = false) String timestamp
    ) {
        return verificationService.processWebhook(
                rawBody,
                new DiditWebhookVerifier.Headers(
                        signatureV2,
                        signature,
                        signatureSimple,
                        timestamp
                )
        );
    }

    @GetMapping(value = "/callback", produces = MediaType.TEXT_HTML_VALUE)
    @Operation(summary = "Mostrar el retorno del flujo alojado de Didit")
    public String callback() {
        return """
                <!doctype html><html lang="es"><head><meta charset="utf-8">
                <meta name="viewport" content="width=device-width,initial-scale=1">
                <title>T4KASH - Verificacion recibida</title></head>
                <body style="font-family:sans-serif;padding:32px;background:#f7f7f4;color:#191919">
                <h1>Verificacion recibida</h1>
                <p>Ya puedes volver a T4KASH. La aplicacion consultara el resultado seguro.</p>
                </body></html>
                """;
    }
}
