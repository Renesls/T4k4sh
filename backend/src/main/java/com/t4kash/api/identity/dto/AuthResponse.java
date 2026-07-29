package com.t4kash.api.identity.dto;

import java.time.LocalDateTime;

public record AuthResponse(
        String token,
        LocalDateTime fechaExpiracion,
        AuthenticatedUserResponse usuario
) {
}
