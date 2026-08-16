package com.t4kash.api.marketplace.repository;

import com.t4kash.api.marketplace.entity.ComentarioEntrega;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ComentarioEntregaRepository extends JpaRepository<ComentarioEntrega, Integer> {
    List<ComentarioEntrega> findByIdEntregaOrderByFechaComentarioAsc(Integer idEntrega);

    List<ComentarioEntrega> findByIdEntregaInOrderByFechaComentarioAsc(
            List<Integer> idEntregas
    );
}
