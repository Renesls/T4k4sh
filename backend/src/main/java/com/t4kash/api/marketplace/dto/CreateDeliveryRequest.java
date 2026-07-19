package com.t4kash.api.marketplace.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateDeliveryRequest(
        @NotBlank
        String descripcionEntrega
) {
}
