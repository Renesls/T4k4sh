package com.t4kash.api.identity.dto;

import java.time.LocalDateTime;

public record PublicProfileResponse(
        PublicIdentityResponse identidad,
        LocalDateTime miembroDesde,
        long publicaciones,
        long trabajosCompletados,
        LocalDateTime proximoCambioNombreUsuario
) {
}
