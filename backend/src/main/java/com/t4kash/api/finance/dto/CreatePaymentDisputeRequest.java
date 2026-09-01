package com.t4kash.api.finance.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreatePaymentDisputeRequest(
        @NotBlank @Size(max = 120) String motivo,
        @NotBlank @Size(min = 10, max = 1000) String descripcion,
        @NotBlank
        @Pattern(
                regexp = "PAGO_ESTUDIANTE|REEMBOLSO_CLIENTE",
                message = "debe ser PAGO_ESTUDIANTE o REEMBOLSO_CLIENTE"
        )
        String solucionSolicitada
) {
}
