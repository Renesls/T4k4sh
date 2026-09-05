package com.t4kash.api.identity.repository;

import com.t4kash.api.identity.entity.EventoWebhookIdentidad;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EventoWebhookIdentidadRepository
        extends JpaRepository<EventoWebhookIdentidad, Integer> {
    Optional<EventoWebhookIdentidad> findByClaveIdempotencia(String claveIdempotencia);
}
