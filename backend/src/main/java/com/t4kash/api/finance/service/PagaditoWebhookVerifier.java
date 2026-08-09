package com.t4kash.api.finance.service;

import com.t4kash.api.exception.PaymentProviderException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.Signature;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.Locale;
import java.util.zip.CRC32;

@Component
public class PagaditoWebhookVerifier {
    private final RestClient restClient;
    private final String wsk;

    public PagaditoWebhookVerifier(
            RestClient.Builder restClientBuilder,
            @Value("${app.pagadito.wsk:}") String wsk
    ) {
        this.restClient = restClientBuilder.build();
        this.wsk = wsk;
    }

    public boolean verify(String rawBody, Headers headers, String eventId) {
        if (wsk.isBlank() || headers.hasBlankValue()) {
            return false;
        }
        URI certificateUri = URI.create(headers.certificateUrl());
        String host = certificateUri.getHost();
        if (!"https".equalsIgnoreCase(certificateUri.getScheme())
                || host == null
                || !(host.equals("pagadito.com") || host.endsWith(".pagadito.com"))) {
            throw new PaymentProviderException("Pagadito envio una direccion de certificado no permitida.");
        }
        try {
            byte[] rawBytes = rawBody.getBytes(StandardCharsets.UTF_8);
            CRC32 crc32 = new CRC32();
            crc32.update(rawBytes);
            String signedData = String.join("|",
                    headers.notificationId(),
                    headers.notificationTimestamp(),
                    eventId,
                    Long.toUnsignedString(crc32.getValue()),
                    wsk
            );
            byte[] certificateBytes = restClient.get()
                    .uri(certificateUri)
                    .retrieve()
                    .body(byte[].class);
            X509Certificate certificate = (X509Certificate) CertificateFactory
                    .getInstance("X.509")
                    .generateCertificate(new ByteArrayInputStream(certificateBytes));
            Signature verifier = Signature.getInstance(normalizeAlgorithm(headers.algorithm()));
            verifier.initVerify(certificate.getPublicKey());
            verifier.update(signedData.getBytes(StandardCharsets.UTF_8));
            return verifier.verify(Base64.getDecoder().decode(headers.signature()));
        } catch (Exception exception) {
            throw new PaymentProviderException("No se pudo validar la firma de Pagadito.", exception);
        }
    }

    private String normalizeAlgorithm(String value) {
        return switch (value.toLowerCase(Locale.ROOT).replace("-", "")) {
            case "sha256withrsa", "sha256withrsaencryption" -> "SHA256withRSA";
            case "sha512withrsa", "sha512withrsaencryption" -> "SHA512withRSA";
            default -> throw new PaymentProviderException("Pagadito envio un algoritmo de firma no permitido.");
        };
    }

    public record Headers(
            String notificationId,
            String notificationTimestamp,
            String algorithm,
            String certificateUrl,
            String signature
    ) {
        boolean hasBlankValue() {
            return notificationId == null || notificationId.isBlank()
                    || notificationTimestamp == null || notificationTimestamp.isBlank()
                    || algorithm == null || algorithm.isBlank()
                    || certificateUrl == null || certificateUrl.isBlank()
                    || signature == null || signature.isBlank();
        }
    }
}
