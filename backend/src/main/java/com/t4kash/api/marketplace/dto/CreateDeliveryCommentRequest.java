package com.t4kash.api.marketplace.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateDeliveryCommentRequest(
        @NotBlank
        @Size(min = 2, max = 700)
        String comentario
) {
}
