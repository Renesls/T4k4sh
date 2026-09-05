package com.t4kash.api.identity.dto;

public record IdentityWebhookResponse(
        boolean recibido,
        boolean duplicado,
        String estado
) { }
