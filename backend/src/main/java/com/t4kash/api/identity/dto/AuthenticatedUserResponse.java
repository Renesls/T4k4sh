package com.t4kash.api.identity.dto;

import java.util.List;

public record AuthenticatedUserResponse(
        Integer idUsuario,
        String nombre,
        String apellido,
        String correo,
        String estadoUsuario,
        List<String> roles
) {
}
