package com.t4kash.api.communication.dto;

import java.time.LocalDateTime;

public record MessageResponse(
        Integer idMensaje,
        Integer idConversacion,
        Integer idUsuarioEmisor,
        String nombreEmisor,
        String contenido,
        LocalDateTime fechaEnvio,
        boolean leido,
        LocalDateTime fechaLectura,
        boolean propio
) {
}
