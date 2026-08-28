package com.t4kash.api.identity.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.t4kash.api.exception.IdentityProviderException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.net.http.HttpClient;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Component
public class DiditClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(DiditClient.class);

    private final RestClient restClient;
    private final boolean enabled;
    private final String apiKey;
    private final String workflowId;
    private final String callbackUrl;

    public DiditClient(
            @Value("${app.didit.api-url:https://verification.didit.me}") String apiUrl,
            @Value("${app.didit.enabled:false}") boolean enabled,
            @Value("${app.didit.api-key:}") String apiKey,
            @Value("${app.didit.workflow-id:}") String workflowId,
            @Value("${app.public-base-url:http://localhost:8080}") String publicBaseUrl
    ) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        JdkClientHttpRequestFactory requestFactory =
                new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofSeconds(20));
        this.restClient = RestClient.builder()
                .baseUrl(apiUrl.replaceAll("/+$", ""))
                .requestFactory(requestFactory)
                .build();
        this.enabled = enabled;
        this.apiKey = apiKey;
        this.workflowId = workflowId;
        this.callbackUrl = publicBaseUrl.replaceAll("/+$", "")
                + "/api/identity-verifications/callback";
    }

    public CreatedSession createSession(UUID publicUserId) {
        requireConfiguration();
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("workflow_id", configuredWorkflowId().toString());
        request.put("vendor_data", vendorData(publicUserId));
        request.put("callback", callbackUrl);
        request.put("callback_method", "both");
        request.put("language", "es");
        try {
            JsonNode response = restClient.post()
                    .uri("/v3/session/")
                    .header("x-api-key", apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(JsonNode.class);
            if (response == null) {
                throw new IdentityProviderException("Didit devolvio una respuesta vacia.");
            }
            return new CreatedSession(
                    requiredUuid(response, "session_id"),
                    requiredUuid(response, "workflow_id"),
                    nullableInteger(response, "workflow_version"),
                    requiredText(response, "url"),
                    requiredText(response, "status")
            );
        } catch (IdentityProviderException exception) {
            throw exception;
        } catch (RestClientResponseException exception) {
            LOGGER.warn("Didit rechazo la creacion de sesion con estado {}.", exception.getStatusCode());
            throw new IdentityProviderException(
                    "Didit rechazo el inicio de la verificacion.",
                    exception
            );
        } catch (RestClientException exception) {
            LOGGER.warn("No se pudo crear la sesion de Didit.", exception);
            throw new IdentityProviderException(
                    "No se pudo conectar con Didit. Intenta nuevamente.",
                    exception
            );
        }
    }

    public Decision getDecision(UUID sessionId) {
        requireConfiguration();
        try {
            JsonNode response = restClient.get()
                    .uri("/v3/session/{sessionId}/decision/", sessionId)
                    .header("x-api-key", apiKey)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(JsonNode.class);
            if (response == null) {
                throw new IdentityProviderException("Didit devolvio una decision vacia.");
            }
            DocumentReference document = extractDocument(response.path("id_verifications"));
            return new Decision(
                    requiredUuid(response, "session_id"),
                    requiredUuid(response, "workflow_id"),
                    nullableInteger(response, "workflow_version"),
                    response.path("vendor_data").asText(null),
                    requiredText(response, "status"),
                    document.documentNumber(),
                    document.issuingState(),
                    parseDateTime(response.path("expires_at").asText(null))
            );
        } catch (IdentityProviderException exception) {
            throw exception;
        } catch (RestClientResponseException exception) {
            LOGGER.warn("Didit rechazo la consulta de decision con estado {}.", exception.getStatusCode());
            throw new IdentityProviderException(
                    "Didit no pudo confirmar el estado de la verificacion.",
                    exception
            );
        } catch (RestClientException exception) {
            LOGGER.warn("No se pudo consultar la decision de Didit.", exception);
            throw new IdentityProviderException(
                    "No se pudo conectar con Didit. Intenta nuevamente.",
                    exception
            );
        }
    }

    static String vendorData(UUID publicUserId) {
        return "usuario:" + publicUserId;
    }

    private DocumentReference extractDocument(JsonNode verifications) {
        if (!verifications.isArray()) {
            return new DocumentReference(null, null);
        }
        JsonNode fallback = null;
        for (JsonNode item : verifications) {
            if (!item.path("document_number").asText("").isBlank()) {
                if (fallback == null) {
                    fallback = item;
                }
                if ("Approved".equalsIgnoreCase(item.path("status").asText())) {
                    return documentReference(item);
                }
            }
        }
        return fallback == null
                ? new DocumentReference(null, null)
                : documentReference(fallback);
    }

    private DocumentReference documentReference(JsonNode item) {
        return new DocumentReference(
                item.path("document_number").asText(null),
                item.path("issuing_state").asText("DESCONOCIDO")
        );
    }

    private void requireConfiguration() {
        if (!enabled || apiKey == null || apiKey.isBlank()
                || workflowId == null || workflowId.isBlank()) {
            throw new IdentityProviderException(
                    "La verificacion de identidad aun no esta configurada en el servidor."
            );
        }
    }

    private UUID configuredWorkflowId() {
        try {
            return UUID.fromString(workflowId.trim());
        } catch (IllegalArgumentException exception) {
            throw new IdentityProviderException(
                    "El identificador del flujo de Didit no es valido.",
                    exception
            );
        }
    }

    private UUID requiredUuid(JsonNode node, String field) {
        try {
            return UUID.fromString(requiredText(node, field));
        } catch (IllegalArgumentException exception) {
            throw new IdentityProviderException(
                    "Didit devolvio un identificador no valido para " + field + ".",
                    exception
            );
        }
    }

    private String requiredText(JsonNode node, String field) {
        String value = node.path(field).asText();
        if (value.isBlank()) {
            throw new IdentityProviderException("Didit no devolvio el campo " + field + ".");
        }
        return value;
    }

    private Integer nullableInteger(JsonNode node, String field) {
        return node.path(field).isIntegralNumber() ? node.path(field).intValue() : null;
    }

    private LocalDateTime parseDateTime(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return OffsetDateTime.parse(value).withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime();
        } catch (Exception ignored) {
            try {
                return LocalDateTime.ofInstant(Instant.parse(value), ZoneOffset.UTC);
            } catch (Exception exception) {
                LOGGER.debug("Didit devolvio una fecha no reconocida.");
                return null;
            }
        }
    }

    public record CreatedSession(
            UUID sessionId,
            UUID workflowId,
            Integer workflowVersion,
            String url,
            String status
    ) { }

    public record Decision(
            UUID sessionId,
            UUID workflowId,
            Integer workflowVersion,
            String vendorData,
            String status,
            String documentNumber,
            String issuingState,
            LocalDateTime expiresAt
    ) { }

    private record DocumentReference(String documentNumber, String issuingState) { }
}
