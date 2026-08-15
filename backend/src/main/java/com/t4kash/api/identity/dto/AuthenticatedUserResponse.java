package com.t4kash.api.identity.dto;

import java.util.List;

public record AuthenticatedUserResponse(
        Integer idUsuario,
        String nombreUsuario,
        String nombre,
        String apellido,
        String correo,
        Integer idUniversidad,
        String nombreUniversidad,
        Integer idCarrera,
        String nombreCarrera,
        String estadoUsuario,
        List<String> roles
) {
}
