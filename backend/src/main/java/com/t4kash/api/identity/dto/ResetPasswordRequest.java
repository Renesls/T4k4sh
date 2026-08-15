package com.t4kash.api.identity.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
        @NotBlank(message = "El correo es obligatorio.")
        @Email(message = "El correo no tiene un formato valido.")
        String correo,

        @NotBlank(message = "El codigo es obligatorio.")
        @Pattern(regexp = "\\d{6}", message = "El codigo debe contener 6 digitos.")
        String codigo,

        @NotBlank(message = "La nueva contrasena es obligatoria.")
        @Size(min = 8, max = 72, message = "La contrasena debe tener entre 8 y 72 caracteres.")
        String nuevaPassword
) {
}
