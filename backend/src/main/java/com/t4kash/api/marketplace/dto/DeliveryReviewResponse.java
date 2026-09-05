package com.t4kash.api.marketplace.dto;

import com.t4kash.api.marketplace.entity.RevisionEntrega;

import java.time.LocalDateTime;

public record DeliveryReviewResponse(
        Integer idRevisionEntrega,
        Integer idEntrega,
        Integer idUsuarioRevisa,
        String resultadoRevision,
        String observacion,
        LocalDateTime fechaRevision,
        String estadoRevision
) {
    public static DeliveryReviewResponse fromEntity(RevisionEntrega revision) {
        return new DeliveryReviewResponse(
                revision.getIdRevisionEntrega(),
                revision.getIdEntrega(),
                revision.getIdUsuarioRevisa(),
                revision.getResultadoRevision(),
                revision.getObservacion(),
                revision.getFechaRevision(),
                revision.getEstadoRevision()
        );
    }
}
