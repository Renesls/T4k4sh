package com.t4kash.api.marketplace.repository;

import com.t4kash.api.marketplace.entity.RevisionEntrega;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RevisionEntregaRepository extends JpaRepository<RevisionEntrega, Integer> {
    List<RevisionEntrega> findByIdEntregaOrderByFechaRevisionAsc(Integer idEntrega);

    List<RevisionEntrega> findByIdEntregaInOrderByFechaRevisionAsc(
            List<Integer> idEntregas
    );
}
