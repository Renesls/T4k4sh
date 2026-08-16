package com.t4kash.api.finance.repository;

import com.t4kash.api.finance.entity.EventoWebhookPago;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EventoWebhookPagoRepository extends JpaRepository<EventoWebhookPago, Long> {
    Optional<EventoWebhookPago> findByProveedorPagoAndEntornoPagoAndIdEventoProveedor(
            String proveedorPago,
            String entornoPago,
            String idEventoProveedor
    );
}
