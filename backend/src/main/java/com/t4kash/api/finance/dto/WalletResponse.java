package com.t4kash.api.finance.dto;

import java.math.BigDecimal;
import java.util.List;

public record WalletResponse(
        String moneda,
        BigDecimal balanceDisponible,
        BigDecimal fondosRetenidos,
        BigDecimal totalGanado,
        List<PaymentResponse> pagos,
        List<WalletMovementResponse> movimientos
) {
}
