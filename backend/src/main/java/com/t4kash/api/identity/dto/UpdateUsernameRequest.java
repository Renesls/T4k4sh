package com.t4kash.api.identity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateUsernameRequest(
        @NotBlank(message = "El nombre de usuario es obligatorio.")
        @Size(min = 3, max = 30, message = "El nombre de usuario debe tener entre 3 y 30 caracteres.")
        @Pattern(
                regexp = "^@?[A-Za-z0-9][A-Za-z0-9._]{2,29}$",
                message = "Usa solo letras, numeros, puntos o guiones bajos."
        )
        String nombreUsuario
) {
}
