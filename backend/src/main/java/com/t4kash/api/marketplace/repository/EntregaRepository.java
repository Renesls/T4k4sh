package com.t4kash.api.marketplace.repository;

import com.t4kash.api.marketplace.entity.Entrega;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EntregaRepository extends JpaRepository<Entrega, Integer> {
    List<Entrega> findByIdTrabajoOrderByFechaEntregaDesc(Integer idTrabajo);

    boolean existsByIdTrabajoAndEstadoEntrega(Integer idTrabajo, String estadoEntrega);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT entrega FROM Entrega entrega WHERE entrega.idEntrega = :idEntrega")
    Optional<Entrega> findByIdForUpdate(@Param("idEntrega") Integer idEntrega);
}
