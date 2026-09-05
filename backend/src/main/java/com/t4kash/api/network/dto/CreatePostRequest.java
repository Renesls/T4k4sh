package com.t4kash.api.network.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreatePostRequest(
        @Size(max = 5000, message = "El contenido no puede superar 5000 caracteres.")
        String contenido,

        @NotBlank(message = "Selecciona el tipo de publicacion.")
        @Size(max = 40)
        String tipoPublicacion,

        @NotBlank(message = "Selecciona la visibilidad.")
        @Size(max = 30)
        String visibilidad,

        Boolean permiteComentarios,

        Integer idPublicacionOrigen
) {
}
