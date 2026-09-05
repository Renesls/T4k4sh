package com.t4kash.api.identity.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.t4kash.api.exception.InvalidWebhookSignatureException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

@Component
public class DiditWebhookVerifier {
    private final ObjectMapper objectMapper;
    private final String secret;
    private final long toleranceSeconds;
    private final Clock clock;

    @Autowired
    public DiditWebhookVerifier(
            ObjectMapper objectMapper,
            @Value("${app.didit.webhook-secret:}") String secret,
            @Value("${app.didit.webhook-tolerance-seconds:300}") long toleranceSeconds
    ) {
        this(objectMapper, secret, toleranceSeconds, Clock.systemUTC());
    }

    DiditWebhookVerifier(
            ObjectMapper objectMapper,
            String secret,
            long toleranceSeconds,
            Clock clock
    ) {
        this.objectMapper = objectMapper;
        this.secret = secret;
        this.toleranceSeconds = toleranceSeconds;
        this.clock = clock;
    }

    public VerificationMethod verify(String rawBody, JsonNode body, Headers headers) {
        requireTimestamp(headers.timestamp());
        if (secret == null || secret.isBlank()) {
            throw new InvalidWebhookSignatureException(
                    "El secreto del webhook de identidad no esta configurado."
            );
        }
        if (matches(headers.signatureV2(), canonicalJson(body))) {
            return VerificationMethod.V2;
        }
        if (matches(headers.signature(), rawBody)) {
            return VerificationMethod.RAW;
        }
        if (matches(headers.signatureSimple(), canonicalSimple(body))) {
            return VerificationMethod.SIMPLE;
        }
        throw new InvalidWebhookSignatureException(
                "La firma del webhook de identidad no es valida."
        );
    }

    private void requireTimestamp(String value) {
        try {
            long timestamp = Long.parseLong(value);
            long difference = Math.abs(clock.instant().getEpochSecond() - timestamp);
            if (difference > toleranceSeconds) {
                throw new InvalidWebhookSignatureException(
                        "El webhook de identidad esta fuera de la ventana permitida."
                );
            }
        } catch (NumberFormatException | NullPointerException exception) {
            throw new InvalidWebhookSignatureException(
                    "El webhook de identidad no contiene una fecha valida."
            );
        }
    }

    private String canonicalJson(JsonNode body) {
        try {
            return objectMapper.writeValueAsString(canonicalize(body));
        } catch (Exception exception) {
            throw new InvalidWebhookSignatureException(
                    "No se pudo validar el contenido del webhook de identidad."
            );
        }
    }

    private JsonNode canonicalize(JsonNode node) {
        if (node.isObject()) {
            ObjectNode sorted = JsonNodeFactory.instance.objectNode();
            List<String> fields = new ArrayList<>();
            node.fieldNames().forEachRemaining(fields::add);
            fields.stream().sorted().forEach(field ->
                    sorted.set(field, canonicalize(node.get(field)))
            );
            return sorted;
        }
        if (node.isArray()) {
            ArrayNode array = JsonNodeFactory.instance.arrayNode();
            node.forEach(item -> array.add(canonicalize(item)));
            return array;
        }
        if (node.isFloatingPointNumber()) {
            BigDecimal value = node.decimalValue().stripTrailingZeros();
            if (value.scale() <= 0) {
                return JsonNodeFactory.instance.numberNode(value.toBigIntegerExact());
            }
        }
        return node.deepCopy();
    }

    private String canonicalSimple(JsonNode body) {
        return String.join(
                ":",
                body.path("timestamp").asText(""),
                body.path("session_id").asText(""),
                body.path("status").asText(""),
                body.path("webhook_type").asText("")
        );
    }

    private boolean matches(String receivedSignature, String content) {
        if (receivedSignature == null || receivedSignature.isBlank()) {
            return false;
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] expected = HexFormat.of().formatHex(
                    mac.doFinal(content.getBytes(StandardCharsets.UTF_8))
            ).getBytes(StandardCharsets.US_ASCII);
            byte[] received = receivedSignature.trim().toLowerCase()
                    .getBytes(StandardCharsets.US_ASCII);
            return MessageDigest.isEqual(expected, received);
        } catch (Exception exception) {
            return false;
        }
    }

    public record Headers(
            String signatureV2,
            String signature,
            String signatureSimple,
            String timestamp
    ) { }

    public enum VerificationMethod {
        V2,
        RAW,
        SIMPLE
    }
}
