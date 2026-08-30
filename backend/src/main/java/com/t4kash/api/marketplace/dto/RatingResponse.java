package com.t4kash.api.marketplace.dto;

import com.t4kash.api.identity.dto.PublicIdentityResponse;
import com.t4kash.api.marketplace.entity.Calificacion;

import java.time.LocalDateTime;

public record RatingResponse(
        Integer idCalificacion,
        Integer idTrabajo,
        Integer idCalificador,
        Integer idCalificado,
        Integer puntuacion,
        String comentario,
        LocalDateTime fechaCalificacion,
        PublicIdentityResponse calificador
) {
    public static RatingResponse fromEntity(Calificacion calificacion) {
        return new RatingResponse(
                calificacion.getIdCalificacion(),
                calificacion.getIdTrabajo(),
                calificacion.getIdCalificador(),
                calificacion.getIdCalificado(),
                calificacion.getPuntuacion(),
                calificacion.getComentario(),
                calificacion.getFechaCalificacion(),
                null
        );
    }

    public RatingResponse withCalificador(PublicIdentityResponse identity) {
        return new RatingResponse(
                idCalificacion, idTrabajo, idCalificador, idCalificado,
                puntuacion, comentario, fechaCalificacion, identity
        );
    }
}
