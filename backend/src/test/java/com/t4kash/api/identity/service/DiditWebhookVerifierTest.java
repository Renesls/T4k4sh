package com.t4kash.api.identity.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.t4kash.api.exception.InvalidWebhookSignatureException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DiditWebhookVerifierTest {
    private static final String SECRET = "didit-webhook-secret-with-at-least-32-characters";
    private static final long NOW = 1_774_970_000L;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private DiditWebhookVerifier verifier;

    @BeforeEach
    void setUp() {
        verifier = new DiditWebhookVerifier(
                objectMapper,
                SECRET,
                300,
                Clock.fixed(Instant.ofEpochSecond(NOW), ZoneOffset.UTC)
        );
    }

    @Test
    void acceptsCanonicalV2Signature() throws Exception {
        String rawBody = """
                {"webhook_type":"status.updated","timestamp":1774970000,"status":"Approved","session_id":"aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee"}
                """.trim();
        JsonNode body = objectMapper.readTree(rawBody);
        String canonical = """
                {"session_id":"aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee","status":"Approved","timestamp":1774970000,"webhook_type":"status.updated"}
                """.trim();

        DiditWebhookVerifier.VerificationMethod method = verifier.verify(
                rawBody,
                body,
                new DiditWebhookVerifier.Headers(
                        sign(canonical),
                        null,
                        null,
                        Long.toString(NOW)
                )
        );

        assertEquals(DiditWebhookVerifier.VerificationMethod.V2, method);
    }

    @Test
    void rejectsAlteredWebhook() throws Exception {
        String rawBody = """
                {"webhook_type":"status.updated","timestamp":1774970000,"status":"Declined","session_id":"aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee"}
                """.trim();

        assertThrows(
                InvalidWebhookSignatureException.class,
                () -> verifier.verify(
                        rawBody,
                        objectMapper.readTree(rawBody),
                        new DiditWebhookVerifier.Headers(
                                sign("contenido-diferente"),
                                null,
                                null,
                                Long.toString(NOW)
                        )
                )
        );
    }

    @Test
    void rejectsExpiredWebhook() throws Exception {
        String rawBody = """
                {"webhook_type":"status.updated","timestamp":1774969000,"status":"Approved","session_id":"aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee"}
                """.trim();

        assertThrows(
                InvalidWebhookSignatureException.class,
                () -> verifier.verify(
                        rawBody,
                        objectMapper.readTree(rawBody),
                        new DiditWebhookVerifier.Headers(
                                null,
                                sign(rawBody),
                                null,
                                Long.toString(NOW - 1000)
                        )
                )
        );
    }

    private String sign(String content) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return HexFormat.of().formatHex(
                mac.doFinal(content.getBytes(StandardCharsets.UTF_8))
        );
    }
}
