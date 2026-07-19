package com.t4kash.api.marketplace.repository;

import com.t4kash.api.marketplace.entity.Entrega;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EntregaRepository extends JpaRepository<Entrega, Integer> {
    List<Entrega> findByIdTrabajoOrderByFechaEntregaDesc(Integer idTrabajo);
}
