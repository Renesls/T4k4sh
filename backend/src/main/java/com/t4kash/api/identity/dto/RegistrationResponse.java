package com.t4kash.api.identity.dto;

import java.time.LocalDateTime;

public record RegistrationResponse(
        String correo,
        LocalDateTime fechaExpiracion,
        String mensaje
) {
}
