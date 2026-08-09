package com.t4kash.api.finance.dto;

import com.t4kash.api.finance.entity.TransaccionPago;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record WalletMovementResponse(
        Long idTransaccion,
        Integer idPago,
        String tipoMovimiento,
        String saldoAfectado,
        BigDecimal monto,
        String moneda,
        String estadoMovimiento,
        String proveedorPago,
        String descripcion,
        LocalDateTime fechaRegistro
) {
    public static WalletMovementResponse fromEntity(TransaccionPago movement) {
        return new WalletMovementResponse(
                movement.getIdTransaccion(), movement.getIdPago(),
                movement.getTipoMovimiento(), movement.getSaldoAfectado(),
                movement.getMonto(), movement.getMoneda(),
                movement.getEstadoMovimiento(), movement.getProveedorPago(),
                movement.getDescripcion(), movement.getFechaRegistro()
        );
    }
}
