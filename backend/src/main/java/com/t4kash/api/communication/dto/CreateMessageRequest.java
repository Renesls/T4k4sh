package com.t4kash.api.communication.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateMessageRequest(
        @NotBlank(message = "Escribe un mensaje.")
        @Size(max = 2000, message = "El mensaje no puede superar 2000 caracteres.")
        String contenido
) {
}
