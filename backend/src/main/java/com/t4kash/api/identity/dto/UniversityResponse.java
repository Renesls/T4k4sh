package com.t4kash.api.identity.dto;

import java.util.List;

public record UniversityResponse(
        Integer idUniversidad,
        String nombreUniversidad,
        List<String> dominiosCorreo
) {
}
