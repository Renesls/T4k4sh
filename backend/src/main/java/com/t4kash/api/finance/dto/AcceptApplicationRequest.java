package com.t4kash.api.finance.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record AcceptApplicationRequest(
        @NotBlank
        @Pattern(regexp = "PAGADITO|EFECTIVO", message = "debe ser PAGADITO o EFECTIVO")
        String metodoPago
) {
}
