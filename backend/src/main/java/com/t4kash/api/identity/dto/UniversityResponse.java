package com.t4kash.api.identity.dto;

public record UniversityResponse(
        Integer idUniversidad,
        String nombreUniversidad,
        String dominioCorreo
) {
}
