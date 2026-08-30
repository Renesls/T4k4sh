package com.t4kash.api.identity.dto;

import com.t4kash.api.marketplace.dto.RatingResponse;

import java.time.LocalDateTime;
import java.util.List;

public record PublicProfileResponse(
        PublicIdentityResponse identidad,
        LocalDateTime miembroDesde,
        long publicaciones,
        long trabajosCompletados,
        LocalDateTime proximoCambioNombreUsuario,
        Double promedioCalificacion,
        long totalCalificaciones,
        String insignia,
        List<RatingResponse> ultimasResenas
) {
}
