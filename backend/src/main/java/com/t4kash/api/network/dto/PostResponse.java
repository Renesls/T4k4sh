package com.t4kash.api.network.dto;

import com.t4kash.api.identity.dto.PublicIdentityResponse;

import java.time.LocalDateTime;
import java.util.Map;

public record PostResponse(
        Integer idPublicacion,
        PublicIdentityResponse autor,
        Integer idPublicacionOrigen,
        String contenido,
        String tipoPublicacion,
        String visibilidad,
        boolean permiteComentarios,
        LocalDateTime fechaPublicacion,
        LocalDateTime fechaEdicion,
        String estadoPublicacion,
        Map<String, Long> reacciones,
        long totalReacciones,
        long totalComentarios,
        long totalCompartidas,
        String miReaccion,
        boolean guardada,
        boolean propia
) {
}
