package com.t4kash.api.moderation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ReviewReportRequest(
        @NotBlank(message = "Selecciona el resultado de la revision.")
        String estadoReporte,

        @Size(max = 700, message = "La observacion no puede superar 700 caracteres.")
        String observacion,

        boolean retirarPublicacion
) {
}
