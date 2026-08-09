package com.t4kash.api.finance.dto;

public record CheckoutResponse(
        Integer idPago,
        String checkoutUrl,
        String estadoPago
) {
}
