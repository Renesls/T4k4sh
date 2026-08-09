package com.t4kash.api.finance.repository;

import com.t4kash.api.finance.entity.EventoWebhookPago;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventoWebhookPagoRepository extends JpaRepository<EventoWebhookPago, Long> {
    boolean existsByProveedorPagoAndEntornoPagoAndIdEventoProveedor(
            String proveedorPago,
            String entornoPago,
            String idEventoProveedor
    );
}
