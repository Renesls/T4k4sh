package com.t4kash.api.identity.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "El correo es obligatorio.")
        @Email(message = "El correo no tiene un formato valido.")
        String correo,

        @NotBlank(message = "La contrasena es obligatoria.")
        String password,

        String fcmToken
) {
}