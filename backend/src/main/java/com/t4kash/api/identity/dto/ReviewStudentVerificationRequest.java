package com.t4kash.api.identity.dto;

import jakarta.validation.constraints.Size;

public record ReviewStudentVerificationRequest(
        @Size(max = 300, message = "La observacion no puede superar 300 caracteres.")
        String observacion
) {
}
