package com.t4kash.api.network.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateCommentRequest(
        @NotBlank(message = "Escribe un comentario.")
        @Size(max = 2000, message = "El comentario no puede superar 2000 caracteres.")
        String contenido
) {
}
