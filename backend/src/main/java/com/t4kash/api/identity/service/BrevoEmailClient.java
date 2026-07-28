package com.t4kash.api.identity.service;

import com.t4kash.api.exception.EmailDeliveryException;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Component
public class BrevoEmailClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(BrevoEmailClient.class);

    private final RestClient restClient;
    private final String apiKey;
    private final String from;
    private final String fromName;

    public BrevoEmailClient(
            @Value("${app.mail.brevo-api-url:https://api.brevo.com}") String apiUrl,
            @Value("${app.mail.brevo-api-key:}") String apiKey,
            @Value("${app.mail.from:}") String from,
            @Value("${app.mail.from-name:T4KASH}") String fromName
    ) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        JdkClientHttpRequestFactory requestFactory =
                new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofSeconds(15));

        this.restClient = RestClient.builder()
                .baseUrl(apiUrl)
                .requestFactory(requestFactory)
                .build();
        this.apiKey = apiKey;
        this.from = from;
        this.fromName = fromName;
    }

    public void sendCode(String recipient, String code, int expirationMinutes) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new EmailDeliveryException(
                    "La clave de la API de correo no esta configurada."
            );
        }
        if (from == null || from.isBlank()) {
            throw new EmailDeliveryException(
                    "El remitente de los correos no esta configurado."
            );
        }

        Map<String, Object> request = Map.of(
                "sender", Map.of("name", fromName, "email", from),
                "to", List.of(Map.of("email", recipient)),
                "subject", "Codigo de verificacion de T4KASH",
                "textContent", """
                        Tu codigo de verificacion es:

                        %s

                        El codigo vence en %d minutos. Si no solicitaste esta cuenta, ignora este mensaje.
                        """.formatted(code, expirationMinutes)
        );

        try {
            restClient.post()
                    .uri("/v3/smtp/email")
                    .header("api-key", apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException ex) {
            LOGGER.warn(
                    "Brevo rechazo el correo de verificacion con estado {}.",
                    ex.getStatusCode()
            );
            throw new EmailDeliveryException(
                    "El proveedor de correo rechazo el envio del codigo.",
                    ex
            );
        } catch (RestClientException ex) {
            LOGGER.warn("No se pudo conectar con la API de Brevo.", ex);
            throw new EmailDeliveryException(
                    "No se pudo conectar con el proveedor de correo.",
                    ex
            );
        }
    }
}
