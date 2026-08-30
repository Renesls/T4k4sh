package com.t4kash.api.marketplace.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateRatingRequest(
        @NotNull(message = "La puntuacion es obligatoria.")
        @Min(value = 1, message = "La puntuacion minima es 1.")
        @Max(value = 5, message = "La puntuacion maxima es 5.")
        Integer puntuacion,

        @Size(max = 500, message = "El comentario no puede superar 500 caracteres.")
        String comentario
) {
}
