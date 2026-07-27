package com.t4kash.api.marketplace.service;

import com.t4kash.api.exception.StorageOperationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriUtils;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class SupabaseStorageClient implements ObjectStorage {
    private final String supabaseUrl;
    private final String secretKey;
    private final String bucketName;
    private final RestClient restClient;

    public SupabaseStorageClient(
            @Value("${app.storage.supabase-url:}") String supabaseUrl,
            @Value("${app.storage.secret-key:}") String secretKey,
            @Value("${app.storage.bucket:t4kash-attachments}") String bucketName,
            RestClient.Builder restClientBuilder
    ) {
        this.supabaseUrl = stripTrailingSlash(supabaseUrl);
        this.secretKey = secretKey.trim();
        this.bucketName = bucketName.trim();
        this.restClient = restClientBuilder.build();
    }

    @Override
    public String bucketName() {
        return bucketName;
    }

    @Override
    public void upload(String objectPath, String contentType, byte[] content) {
        ensureConfigured();
        try {
            restClient.post()
                    .uri(objectUri(objectPath, false))
                    .headers(this::addAuthenticationHeaders)
                    .header("x-upsert", "false")
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(content)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException ex) {
            throw storageError("No se pudo subir el archivo a Supabase Storage.", ex);
        }
    }

    @Override
    public byte[] download(String objectPath) {
        ensureConfigured();
        try {
            byte[] content = restClient.get()
                    .uri(objectUri(objectPath, true))
                    .headers(this::addAuthenticationHeaders)
                    .retrieve()
                    .body(byte[].class);
            if (content == null) {
                throw new StorageOperationException("Supabase Storage devolvio un archivo vacio.");
            }
            return content;
        } catch (RestClientResponseException ex) {
            throw storageError("No se pudo descargar el archivo de Supabase Storage.", ex);
        }
    }

    @Override
    public void delete(String objectPath) {
        if (!isConfigured()) {
            return;
        }
        try {
            restClient.method(HttpMethod.DELETE)
                    .uri(URI.create(supabaseUrl + "/storage/v1/object/" + encode(bucketName)))
                    .headers(this::addAuthenticationHeaders)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("prefixes", List.of(objectPath)))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RuntimeException ignored) {
            // Limpieza de respaldo si falla el guardado de metadatos.
        }
    }

    private URI objectUri(String objectPath, boolean authenticated) {
        String route = authenticated ? "/object/authenticated/" : "/object/";
        return URI.create(
                supabaseUrl + "/storage/v1" + route +
                        encode(bucketName) + "/" + encodePath(objectPath)
        );
    }

    private String encodePath(String path) {
        return Stream.of(path.split("/"))
                .map(this::encode)
                .collect(Collectors.joining("/"));
    }

    private String encode(String value) {
        return UriUtils.encodePathSegment(value, StandardCharsets.UTF_8);
    }

    private void ensureConfigured() {
        if (!isConfigured()) {
            throw new StorageOperationException(
                    "Supabase Storage no esta configurado en el backend."
            );
        }
    }

    private void addAuthenticationHeaders(HttpHeaders headers) {
        headers.set("apikey", secretKey);
        if (!secretKey.startsWith("sb_secret_")) {
            headers.setBearerAuth(secretKey);
        }
    }

    private boolean isConfigured() {
        return !supabaseUrl.isBlank() && !secretKey.isBlank() && !bucketName.isBlank();
    }

    private StorageOperationException storageError(
            String message,
            RestClientResponseException cause
    ) {
        String detail = cause.getResponseBodyAsString();
        if (detail == null || detail.isBlank()) {
            detail = "HTTP " + cause.getStatusCode().value();
        }
        return new StorageOperationException(message + " " + detail, cause);
    }

    private static String stripTrailingSlash(String value) {
        String result = value == null ? "" : value.trim();
        while (result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }
}
