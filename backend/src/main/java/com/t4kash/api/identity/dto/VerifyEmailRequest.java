package com.t4kash.api.identity.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record VerifyEmailRequest(
        @NotBlank(message = "El correo es obligatorio.")
        @Email(message = "El correo no tiene un formato valido.")
        String correo,

        @NotBlank(message = "El codigo es obligatorio.")
        @Pattern(regexp = "\\d{6}", message = "El codigo debe contener 6 digitos.")
        String codigo
) {
}
