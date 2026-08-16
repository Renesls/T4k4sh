package com.t4kash.api.marketplace.dto;

import com.t4kash.api.marketplace.entity.ComentarioEntrega;

import java.time.LocalDateTime;

public record DeliveryCommentResponse(
        Integer idComentarioEntrega,
        Integer idEntrega,
        Integer idUsuario,
        String comentario,
        String tipoComentario,
        LocalDateTime fechaComentario
) {
    public static DeliveryCommentResponse fromEntity(ComentarioEntrega comentario) {
        return new DeliveryCommentResponse(
                comentario.getIdComentarioEntrega(),
                comentario.getIdEntrega(),
                comentario.getIdUsuario(),
                comentario.getComentario(),
                comentario.getTipoComentario(),
                comentario.getFechaComentario()
        );
    }
}
