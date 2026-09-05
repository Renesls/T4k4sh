package com.t4kash.api.finance.dto;

import com.t4kash.api.finance.entity.DisputaPago;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentDisputeResponse(
        Integer idDisputa,
        Integer idPago,
        Integer idUsuarioAbre,
        Integer idAdminAsignado,
        String motivo,
        String descripcion,
        String solucionSolicitada,
        BigDecimal montoDisputado,
        String estadoDisputa,
        String prioridad,
        LocalDateTime fechaApertura,
        LocalDateTime fechaLimiteRespuesta,
        LocalDateTime fechaActualizacion,
        LocalDateTime fechaResolucion,
        String resolucion
) {
    public static PaymentDisputeResponse fromEntity(DisputaPago disputa) {
        return new PaymentDisputeResponse(
                disputa.getIdDisputa(), disputa.getIdPago(), disputa.getIdUsuarioAbre(),
                disputa.getIdAdminAsignado(), disputa.getMotivo(), disputa.getDescripcion(),
                disputa.getSolucionSolicitada(), disputa.getMontoDisputado(),
                disputa.getEstadoDisputa(), disputa.getPrioridad(), disputa.getFechaApertura(),
                disputa.getFechaLimiteRespuesta(), disputa.getFechaActualizacion(),
                disputa.getFechaResolucion(), disputa.getResolucion()
        );
    }
}
