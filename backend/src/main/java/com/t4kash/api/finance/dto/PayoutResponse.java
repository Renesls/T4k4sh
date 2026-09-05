package com.t4kash.api.finance.dto;

import com.t4kash.api.finance.entity.DesembolsoPago;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PayoutResponse(
        Integer idDesembolso,
        Integer idPago,
        Integer idEstudiante,
        BigDecimal montoDesembolso,
        String moneda,
        String proveedorDesembolso,
        String estadoDesembolso,
        LocalDateTime fechaCreacion,
        LocalDateTime fechaConfirmacion
) {
    public static PayoutResponse fromEntity(DesembolsoPago desembolso) {
        return new PayoutResponse(
                desembolso.getIdDesembolso(), desembolso.getIdPago(),
                desembolso.getIdEstudiante(), desembolso.getMontoDesembolso(),
                desembolso.getMoneda(), desembolso.getProveedorDesembolso(),
                desembolso.getEstadoDesembolso(), desembolso.getFechaCreacion(),
                desembolso.getFechaConfirmacion()
        );
    }
}
