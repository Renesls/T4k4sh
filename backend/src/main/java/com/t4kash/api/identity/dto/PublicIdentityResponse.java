package com.t4kash.api.identity.dto;

public record PublicIdentityResponse(
        Integer idUsuario,
        String nombreUsuario,
        String nombreCompleto,
        String nombreUniversidad,
        String nombreCarrera,
        boolean estudianteVerificado
) {
}
