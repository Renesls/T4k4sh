package com.t4kash.api.identity.service;

import com.t4kash.api.exception.IdentityProviderException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.Locale;

@Component
public class IdentityDocumentHasher {
    private final String secret;

    public IdentityDocumentHasher(
            @Value("${app.didit.document-hash-secret:}") String secret
    ) {
        this.secret = secret;
    }

    public String hash(String issuingState, String documentNumber) {
        if (secret == null || secret.length() < 32) {
            throw new IdentityProviderException(
                    "La proteccion de documentos KYC no esta configurada correctamente."
            );
        }
        String normalized = normalize(issuingState) + ":" + normalize(documentNumber);
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(
                    mac.doFinal(normalized.getBytes(StandardCharsets.UTF_8))
            );
        } catch (Exception exception) {
            throw new IdentityProviderException(
                    "No se pudo proteger el identificador del documento.",
                    exception
            );
        }
    }

    private String normalize(String value) {
        return (value == null ? "" : value)
                .trim()
                .toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9]", "");
    }
}
