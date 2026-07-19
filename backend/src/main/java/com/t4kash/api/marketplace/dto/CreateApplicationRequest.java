package com.t4kash.api.marketplace.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreateApplicationRequest(
        @NotNull
        Integer idEstudiante,

        @Size(max = 500)
        String mensaje,

        @DecimalMin(value = "0.0", inclusive = true)
        BigDecimal precioPropuesto
) {
}
