package com.t4kash.api.finance.dto;

import com.t4kash.api.finance.entity.Pago;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentResponse(
        Integer idPago,
        Integer idTrabajo,
        Integer idCliente,
        Integer idEstudiante,
        String proveedorPago,
        String entornoPago,
        String metodoPago,
        String monedaCobro,
        BigDecimal montoEstudiante,
        BigDecimal porcentajeComisionPlataforma,
        BigDecimal comisionPlataforma,
        BigDecimal comisionProcesador,
        BigDecimal impuestoProcesador,
        BigDecimal montoTotalCliente,
        String estadoPago,
        String referenciaComercio,
        LocalDateTime fechaCreacion,
        LocalDateTime fechaActualizacion,
        LocalDateTime fechaExpiracion,
        LocalDateTime fechaConfirmacion,
        LocalDateTime fechaLiberacion,
        boolean puedePagar
) {
    public static PaymentResponse fromEntity(Pago pago, Integer currentUserId) {
        boolean activeCheckout = "PENDIENTE_PAGO".equals(pago.getEstadoPago())
                && pago.getFechaExpiracion() != null
                && pago.getFechaExpiracion().isAfter(LocalDateTime.now());
        boolean payable = pago.getIdCliente().equals(currentUserId)
                && "PAGADITO".equals(pago.getMetodoPago())
                && !activeCheckout
                && switch (pago.getEstadoPago()) {
                    case "PENDIENTE_PAGO", "PAGO_FALLIDO", "PAGO_CANCELADO",
                            "PAGO_EXPIRADO", "PAGO_REVOCADO" -> true;
                    default -> false;
                };
        return new PaymentResponse(
                pago.getIdPago(), pago.getIdTrabajo(), pago.getIdCliente(),
                pago.getIdEstudiante(), pago.getProveedorPago(), pago.getEntornoPago(),
                pago.getMetodoPago(), pago.getMonedaCobro(), pago.getMontoEstudiante(),
                pago.getPorcentajeComisionPlataforma(), pago.getComisionPlataforma(),
                pago.getComisionProcesador(), pago.getImpuestoProcesador(),
                pago.getMontoTotalCliente(), pago.getEstadoPago(),
                pago.getReferenciaComercio(), pago.getFechaCreacion(),
                pago.getFechaActualizacion(), pago.getFechaExpiracion(),
                pago.getFechaConfirmacion(), pago.getFechaLiberacion(), payable
        );
    }

    public static PaymentResponse fromEntity(Pago pago) {
        return fromEntity(pago, -1);
    }
}
