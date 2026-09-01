package com.t4kash.api.marketplace.dto;

import com.t4kash.api.marketplace.entity.Entrega;

import java.time.LocalDateTime;
import java.util.List;

public record DeliveryResponse(
        Integer idEntrega,
        Integer idTrabajo,
        String descripcionEntrega,
        LocalDateTime fechaEntrega,
        String estadoEntrega,
        List<DeliveryCommentResponse> comentarios,
        List<DeliveryReviewResponse> revisiones
) {
    public static DeliveryResponse fromEntity(Entrega entrega) {
        return fromEntity(entrega, List.of(), List.of());
    }

    public static DeliveryResponse fromEntity(
            Entrega entrega,
            List<DeliveryCommentResponse> comentarios,
            List<DeliveryReviewResponse> revisiones
    ) {
        return new DeliveryResponse(
                entrega.getIdEntrega(),
                entrega.getIdTrabajo(),
                entrega.getDescripcionEntrega(),
                entrega.getFechaEntrega(),
                entrega.getEstadoEntrega(),
                comentarios,
                revisiones
        );
    }
}
