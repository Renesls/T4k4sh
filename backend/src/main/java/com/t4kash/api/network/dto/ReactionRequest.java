package com.t4kash.api.network.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ReactionRequest(
        @NotBlank(message = "Selecciona una reaccion.")
        @Size(max = 30)
        String tipoReaccion
) {
}
