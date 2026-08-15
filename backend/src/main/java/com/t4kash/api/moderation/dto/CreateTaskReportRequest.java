package com.t4kash.api.moderation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateTaskReportRequest(
        @NotBlank(message = "Selecciona una categoria.")
        String categoriaReporte,

        @Size(max = 700, message = "La descripcion no puede superar 700 caracteres.")
        String descripcion
) {
}
