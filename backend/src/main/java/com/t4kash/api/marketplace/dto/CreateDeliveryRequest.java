package com.t4kash.api.marketplace.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateDeliveryRequest(
        @NotBlank
        @Size(min = 10, max = 1000)
        String descripcionEntrega
) {
}
