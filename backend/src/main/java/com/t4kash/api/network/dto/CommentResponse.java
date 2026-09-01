package com.t4kash.api.network.dto;

import com.t4kash.api.identity.dto.PublicIdentityResponse;

import java.time.LocalDateTime;

public record CommentResponse(
        Integer idComentario,
        Integer idPublicacion,
        Integer idComentarioPadre,
        PublicIdentityResponse autor,
        String contenido,
        LocalDateTime fechaComentario,
        LocalDateTime fechaEdicion,
        boolean propio
) {
}
