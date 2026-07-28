package com.t4kash.api.identity.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "El nombre es obligatorio.")
        @Size(max = 80, message = "El nombre no puede superar 80 caracteres.")
        String nombre,

        @NotBlank(message = "El apellido es obligatorio.")
        @Size(max = 80, message = "El apellido no puede superar 80 caracteres.")
        String apellido,

        @NotBlank(message = "El correo es obligatorio.")
        @Email(message = "El correo no tiene un formato valido.")
        @Size(max = 150, message = "El correo no puede superar 150 caracteres.")
        String correo,

        @NotBlank(message = "La contrasena es obligatoria.")
        @Size(min = 8, max = 72, message = "La contrasena debe tener entre 8 y 72 caracteres.")
        String password,

        @NotNull(message = "La universidad es obligatoria.")
        Integer idUniversidad,

        @NotNull(message = "La carrera es obligatoria.")
        Integer idCarrera
) {
}
