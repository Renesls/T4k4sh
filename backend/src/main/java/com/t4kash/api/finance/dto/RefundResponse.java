package com.t4kash.api.finance.dto;

import com.t4kash.api.finance.entity.ReembolsoPago;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record RefundResponse(
        Integer idReembolso,
        Integer idPago,
        Integer idDisputa,
        BigDecimal montoReembolso,
        String moneda,
        String motivo,
        String estadoReembolso,
        LocalDateTime fechaSolicitud,
        LocalDateTime fechaConfirmacion
) {
    public static RefundResponse fromEntity(ReembolsoPago reembolso) {
        return new RefundResponse(
                reembolso.getIdReembolso(), reembolso.getIdPago(), reembolso.getIdDisputa(),
                reembolso.getMontoReembolso(), reembolso.getMoneda(), reembolso.getMotivo(),
                reembolso.getEstadoReembolso(), reembolso.getFechaSolicitud(),
                reembolso.getFechaConfirmacion()
        );
    }
}
