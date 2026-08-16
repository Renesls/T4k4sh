package com.t4kash.api.finance.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ResolvePaymentDisputeRequest(
        @NotBlank
        @Pattern(
                regexp = "LIBERAR_ESTUDIANTE|REEMBOLSAR_CLIENTE",
                message = "debe ser LIBERAR_ESTUDIANTE o REEMBOLSAR_CLIENTE"
        )
        String decision,
        @NotBlank @Size(min = 10, max = 1000) String resolucion
) {
}
