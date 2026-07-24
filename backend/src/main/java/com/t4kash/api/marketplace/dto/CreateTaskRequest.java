package com.t4kash.api.marketplace.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CreateTaskRequest(
        @NotBlank @Size(max = 150)
        String titulo,

        @NotBlank
        String descripcion,

        @NotNull @DecimalMin(value = "0.0", inclusive = true)
        BigDecimal presupuesto,

        LocalDateTime fechaLimitePostulacion,

        LocalDateTime fechaLimite,

        @NotNull
        Integer idCategoria,

        @NotNull
        Integer idCliente,

        @NotBlank @Size(max = 50)
        String tipoOportunidad,

        @Size(max = 30)
        String modalidad,

        @Size(max = 30)
        String visibilidad,

        @Size(max = 250)
        String direccionReferencia,

        @DecimalMin(value = "-90.0") @DecimalMax(value = "90.0")
        BigDecimal latitud,

        @DecimalMin(value = "-180.0") @DecimalMax(value = "180.0")
        BigDecimal longitud
) {
}
