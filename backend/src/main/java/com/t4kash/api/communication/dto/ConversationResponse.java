package com.t4kash.api.communication.dto;

import java.time.LocalDateTime;

public record ConversationResponse(
        Integer idConversacion,
        Integer idTarea,
        Integer idTrabajo,
        String tituloTarea,
        Integer idParticipante,
        String nombreParticipante,
        String nombreUsuarioParticipante,
        String estadoConversacion,
        String ultimoMensaje,
        LocalDateTime fechaUltimoMensaje,
        long mensajesNoLeidos
) {
}
