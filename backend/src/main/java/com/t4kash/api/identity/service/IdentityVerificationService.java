package com.t4kash.api.identity.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.t4kash.api.exception.IdentityProviderException;
import com.t4kash.api.exception.InvalidWebhookSignatureException;
import com.t4kash.api.exception.ResourceConflictException;
import com.t4kash.api.exception.ResourceNotFoundException;
import com.t4kash.api.identity.dto.IdentityVerificationSessionResponse;
import com.t4kash.api.identity.dto.IdentityVerificationStatusResponse;
import com.t4kash.api.identity.dto.IdentityWebhookResponse;
import com.t4kash.api.identity.entity.EventoWebhookIdentidad;
import com.t4kash.api.identity.entity.VerificacionIdentidad;
import com.t4kash.api.identity.repository.EventoWebhookIdentidadRepository;
import com.t4kash.api.identity.repository.VerificacionIdentidadRepository;
import com.t4kash.api.marketplace.entity.Usuario;
import com.t4kash.api.marketplace.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
public class IdentityVerificationService {
    private static final String PROVIDER = "DIDIT";
    private static final String APPROVED = "APROBADA";
    private static final Set<String> ACTIVE_STATES = Set.of(
            "PENDIENTE", "EN_PROCESO", "EN_REVISION", "REQUIERE_ACCION"
    );
    private static final Set<String> ALLOWED_ORIGINS = Set.of(
            "PERFIL", "WALLET", "PAGO", "TRABAJO", "TAREA_RAPIDA", "ADMIN"
    );
    private static final Set<String> SESSION_EVENTS = Set.of(
            "status.updated", "data.updated"
    );

    private final VerificacionIdentidadRepository verificationRepository;
    private final EventoWebhookIdentidadRepository webhookRepository;
    private final UsuarioRepository userRepository;
    private final DiditClient diditClient;
    private final DiditWebhookVerifier webhookVerifier;
    private final IdentityDocumentHasher documentHasher;
    private final IdentityVerificationPolicyService policyService;
    private final ObjectMapper objectMapper;

    public IdentityVerificationService(
            VerificacionIdentidadRepository verificationRepository,
            EventoWebhookIdentidadRepository webhookRepository,
            UsuarioRepository userRepository,
            DiditClient diditClient,
            DiditWebhookVerifier webhookVerifier,
            IdentityDocumentHasher documentHasher,
            IdentityVerificationPolicyService policyService,
            ObjectMapper objectMapper
    ) {
        this.verificationRepository = verificationRepository;
        this.webhookRepository = webhookRepository;
        this.userRepository = userRepository;
        this.diditClient = diditClient;
        this.webhookVerifier = webhookVerifier;
        this.documentHasher = documentHasher;
        this.policyService = policyService;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public IdentityVerificationStatusResponse getCurrent(Integer userId) {
        return verificationRepository
                .findFirstByIdUsuarioOrderByFechaInicioDesc(userId)
                .map(verification -> IdentityVerificationStatusResponse.fromEntity(
                        verification,
                        policyService.isApprovedNow(verification)
                ))
                .orElseGet(IdentityVerificationStatusResponse::notStarted);
    }

    @Transactional
    public IdentityVerificationSessionResponse start(Integer userId, String requestedOrigin) {
        String origin = normalizeOrigin(requestedOrigin);
        Usuario user = requireUser(userId);
        if (user.getUuidPublico() == null) {
            throw new ResourceConflictException(
                    "La cuenta no tiene un identificador publico para iniciar la verificacion."
            );
        }
        Optional<VerificacionIdentidad> latest = verificationRepository
                .findFirstByIdUsuarioOrderByFechaInicioDesc(userId);
        if (latest.filter(policyService::isApprovedNow).isPresent()) {
            VerificacionIdentidad approved = latest.get();
            return new IdentityVerificationSessionResponse(
                    approved.getIdSesionProveedor(),
                    null,
                    APPROVED
            );
        }

        DiditClient.CreatedSession created = diditClient.createSession(user.getUuidPublico());
        VerificacionIdentidad verification = verificationRepository
                .findByIdSesionProveedor(created.sessionId())
                .orElse(null);
        if (verification != null && !verification.getIdUsuario().equals(userId)) {
            throw new IdentityProviderException(
                    "Didit devolvio una sesion asociada a otra cuenta."
            );
        }
        if (verification == null) {
            cancelPreviousActive(userId, created.sessionId());
            verification = new VerificacionIdentidad();
            verification.setIdUsuario(userId);
            verification.setProveedor(PROVIDER);
            verification.setIdSesionProveedor(created.sessionId());
            verification.setOrigenSolicitud(origin);
            verification.setFechaInicio(LocalDateTime.now());
        }
        verification.setIdFlujoProveedor(created.workflowId());
        verification.setVersionFlujo(created.workflowVersion());
        verification.setFechaActualizacion(LocalDateTime.now());

        if ("Approved".equalsIgnoreCase(created.status())) {
            applyDecision(verification, diditClient.getDecision(created.sessionId()));
        } else {
            applyProviderState(verification, created.status());
        }
        VerificacionIdentidad saved = verificationRepository.save(verification);
        return new IdentityVerificationSessionResponse(
                saved.getIdSesionProveedor(),
                created.url(),
                saved.getEstadoVerificacion()
        );
    }

    @Transactional
    public IdentityVerificationStatusResponse refresh(Integer userId) {
        VerificacionIdentidad verification = verificationRepository
                .findFirstByIdUsuarioOrderByFechaInicioDesc(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Aun no has iniciado una verificacion de identidad."
                ));
        applyDecision(
                verification,
                diditClient.getDecision(verification.getIdSesionProveedor())
        );
        VerificacionIdentidad saved = verificationRepository.save(verification);
        return IdentityVerificationStatusResponse.fromEntity(
                saved,
                policyService.isApprovedNow(saved)
        );
    }

    @Transactional(noRollbackFor = IdentityProviderException.class)
    public IdentityWebhookResponse processWebhook(
            String rawBody,
            DiditWebhookVerifier.Headers headers
    ) {
        JsonNode body = readJson(rawBody);
        webhookVerifier.verify(rawBody, body, headers);
        UUID sessionId = requiredUuid(body, "session_id");
        String type = requiredText(body, "webhook_type");
        String reportedStatus = body.path("status").asText(null);
        VerificacionIdentidad verification = verificationRepository
                .findByIdSesionProveedor(sessionId)
                .orElse(null);
        if (verification == null) {
            return new IdentityWebhookResponse(true, false, "IGNORADO");
        }

        String contentHash = sha256(rawBody);
        String providerEventId = body.path("event_id").asText(null);
        String idempotencyKey = sha256(
                providerEventId == null || providerEventId.isBlank()
                        ? String.join(
                            ":",
                            body.path("timestamp").asText(""),
                            sessionId.toString(),
                            reportedStatus == null ? "" : reportedStatus,
                            type
                        )
                        : providerEventId
        );
        EventoWebhookIdentidad event = webhookRepository
                .findByClaveIdempotencia(idempotencyKey)
                .orElse(null);
        if (event != null && !event.getHashContenido().equals(contentHash)) {
            throw new InvalidWebhookSignatureException(
                    "El identificador del webhook fue reutilizado con otro contenido."
            );
        }
        if (event != null && "PROCESADO".equals(event.getEstadoProcesamiento())) {
            return new IdentityWebhookResponse(true, true, "PROCESADO");
        }
        if (event == null) {
            event = new EventoWebhookIdentidad();
            event.setIdVerificacionIdentidad(verification.getIdVerificacionIdentidad());
            event.setIdEventoProveedor(clean(providerEventId));
            event.setClaveIdempotencia(idempotencyKey);
            event.setTipoEvento(type);
            event.setEstadoReportado(clean(reportedStatus));
            event.setFirmaValida(true);
            event.setHashContenido(contentHash);
            event.setFechaRecepcion(LocalDateTime.now());
        }
        event.setEstadoProcesamiento("PENDIENTE");
        event.setDetalleError(null);
        event.setFechaProcesamiento(null);
        webhookRepository.save(event);

        if (!SESSION_EVENTS.contains(type)) {
            event.setEstadoProcesamiento("IGNORADO");
            event.setFechaProcesamiento(LocalDateTime.now());
            webhookRepository.save(event);
            return new IdentityWebhookResponse(true, false, "IGNORADO");
        }

        try {
            applyDecision(verification, diditClient.getDecision(sessionId));
            verificationRepository.save(verification);
            event.setEstadoProcesamiento("PROCESADO");
            event.setFechaProcesamiento(LocalDateTime.now());
            webhookRepository.save(event);
            return new IdentityWebhookResponse(true, false, "PROCESADO");
        } catch (IdentityProviderException exception) {
            event.setEstadoProcesamiento("ERROR");
            event.setFechaProcesamiento(LocalDateTime.now());
            event.setDetalleError("No fue posible consultar la decision autoritativa de Didit.");
            webhookRepository.save(event);
            throw exception;
        }
    }

    private void applyDecision(
            VerificacionIdentidad verification,
            DiditClient.Decision decision
    ) {
        if (!verification.getIdSesionProveedor().equals(decision.sessionId())) {
            throw new IdentityProviderException("Didit devolvio una decision de otra sesion.");
        }
        Usuario user = requireUser(verification.getIdUsuario());
        String expectedVendor = DiditClient.vendorData(user.getUuidPublico());
        if (!expectedVendor.equals(decision.vendorData())) {
            throw new IdentityProviderException(
                    "Didit devolvio una decision asociada a otra identidad interna."
            );
        }

        verification.setIdFlujoProveedor(decision.workflowId());
        verification.setVersionFlujo(decision.workflowVersion());
        verification.setFechaExpiracion(decision.expiresAt());
        applyProviderState(verification, decision.status());
        if (APPROVED.equals(verification.getEstadoVerificacion())) {
            approveDecision(verification, decision);
        } else if ("RECHAZADA".equals(verification.getEstadoVerificacion())) {
            verification.setFechaDecision(LocalDateTime.now());
            verification.setMotivoResultado(
                    "El proveedor no pudo aprobar la verificacion de identidad."
            );
        } else {
            verification.setFechaDecision(null);
            verification.setMotivoResultado(null);
        }
        verification.setFechaActualizacion(LocalDateTime.now());
    }

    private void approveDecision(
            VerificacionIdentidad verification,
            DiditClient.Decision decision
    ) {
        if (decision.documentNumber() == null || decision.documentNumber().isBlank()) {
            verification.setEstadoVerificacion("EN_REVISION");
            verification.setFechaDecision(null);
            verification.setMotivoResultado(
                    "La decision aprobada no incluyo una referencia de documento."
            );
            return;
        }
        String fingerprint = documentHasher.hash(
                decision.issuingState(),
                decision.documentNumber()
        );
        Optional<VerificacionIdentidad> duplicate = verificationRepository
                .findFirstByHuellaDocumentoAndEstadoVerificacion(fingerprint, APPROVED);
        if (duplicate.isPresent()
                && !duplicate.get().getIdUsuario().equals(verification.getIdUsuario())) {
            verification.setEstadoVerificacion("EN_REVISION");
            verification.setFechaDecision(null);
            verification.setHuellaDocumento(null);
            verification.setMotivoResultado(
                    "La identidad requiere una revision administrativa."
            );
            return;
        }
        verification.setHuellaDocumento(fingerprint);
        verification.setFechaDecision(LocalDateTime.now());
        verification.setMotivoResultado(null);
    }

    private void applyProviderState(
            VerificacionIdentidad verification,
            String providerState
    ) {
        String state = switch (providerState == null ? "" : providerState) {
            case "Not Started" -> "PENDIENTE";
            case "In Progress" -> "EN_PROCESO";
            case "Approved" -> APPROVED;
            case "Declined" -> "RECHAZADA";
            case "In Review", "Resubmitted" -> "EN_REVISION";
            case "Expired" -> "EXPIRADA";
            case "Kyc Expired" -> "VENCIDA";
            case "Abandoned" -> "ABANDONADA";
            case "Awaiting User" -> "REQUIERE_ACCION";
            default -> "EN_REVISION";
        };
        verification.setEstadoProveedor(
                providerState == null || providerState.isBlank()
                        ? "Unknown"
                        : providerState
        );
        verification.setEstadoVerificacion(state);
        verification.setFechaActualizacion(LocalDateTime.now());
    }

    private void cancelPreviousActive(Integer userId, UUID newSessionId) {
        verificationRepository
                .findFirstByIdUsuarioAndEstadoVerificacionInOrderByFechaInicioDesc(
                        userId,
                        ACTIVE_STATES
                )
                .filter(item -> !item.getIdSesionProveedor().equals(newSessionId))
                .ifPresent(item -> {
                    item.setEstadoVerificacion("CANCELADA");
                    item.setEstadoProveedor("Superseded");
                    item.setFechaActualizacion(LocalDateTime.now());
                    verificationRepository.save(item);
                });
    }

    private Usuario requireUser(Integer userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "La cuenta autenticada ya no existe."
                ));
    }

    private String normalizeOrigin(String value) {
        String origin = value == null || value.isBlank()
                ? "PERFIL"
                : value.trim().toUpperCase(Locale.ROOT);
        if (!ALLOWED_ORIGINS.contains(origin)) {
            throw new IllegalArgumentException("El origen de la verificacion no es valido.");
        }
        return origin;
    }

    private JsonNode readJson(String rawBody) {
        try {
            return objectMapper.readTree(rawBody);
        } catch (Exception exception) {
            throw new IllegalArgumentException(
                    "El webhook de identidad no contiene un JSON valido."
            );
        }
    }

    private UUID requiredUuid(JsonNode node, String field) {
        try {
            return UUID.fromString(requiredText(node, field));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                    "El webhook no contiene un " + field + " valido."
            );
        }
    }

    private String requiredText(JsonNode node, String field) {
        String value = node.path(field).asText();
        if (value.isBlank()) {
            throw new IllegalArgumentException("El webhook no contiene " + field + ".");
        }
        return value;
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(
                    digest.digest(value.getBytes(StandardCharsets.UTF_8))
            );
        } catch (Exception exception) {
            throw new IllegalStateException("No se pudo calcular el hash de auditoria.", exception);
        }
    }

    private String clean(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
