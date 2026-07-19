package com.t4kash.api.marketplace.dto;

import com.t4kash.api.marketplace.entity.Entrega;

import java.time.LocalDateTime;

public record DeliveryResponse(
        Integer idEntrega,
        Integer idTrabajo,
        String descripcionEntrega,
        LocalDateTime fechaEntrega,
        String estadoEntrega
) {
    public static DeliveryResponse fromEntity(Entrega entrega) {
        return new DeliveryResponse(
                entrega.getIdEntrega(),
                entrega.getIdTrabajo(),
                entrega.getDescripcionEntrega(),
                entrega.getFechaEntrega(),
                entrega.getEstadoEntrega()
        );
    }
}
