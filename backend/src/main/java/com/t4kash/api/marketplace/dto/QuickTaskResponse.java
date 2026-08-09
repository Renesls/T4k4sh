package com.t4kash.api.marketplace.dto;

import com.t4kash.api.identity.dto.PublicIdentityResponse;

public record QuickTaskResponse(
        TaskResponse tarea,
        double distanciaKm,
        long segundosRestantes
) {
    public QuickTaskResponse withClient(PublicIdentityResponse client) {
        return new QuickTaskResponse(
                tarea.withClient(client),
                distanciaKm,
                segundosRestantes
        );
    }
}
