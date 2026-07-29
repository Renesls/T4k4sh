package com.t4kash.api.identity.dto;

import java.time.LocalDateTime;

public record LoginChallengeResponse(
        String correo,
        LocalDateTime fechaExpiracion,
        String mensaje
) {
}
