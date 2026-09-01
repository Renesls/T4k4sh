package com.t4kash.api.finance.repository;

import com.t4kash.api.finance.entity.DisputaPago;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DisputaPagoRepository extends JpaRepository<DisputaPago, Integer> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT disputa FROM DisputaPago disputa WHERE disputa.idDisputa = :idDisputa")
    Optional<DisputaPago> findByIdForUpdate(@Param("idDisputa") Integer idDisputa);

    boolean existsByIdPagoAndEstadoDisputaIn(Integer idPago, List<String> estados);

    @Query(value = """
            SELECT disputa.*
            FROM disputas_pago disputa
            INNER JOIN pagos pago ON pago.id_pago = disputa.id_pago
            WHERE pago.id_cliente = :idUsuario OR pago.id_estudiante = :idUsuario
            ORDER BY disputa.fecha_actualizacion DESC
            """, nativeQuery = true)
    List<DisputaPago> findVisibleToUser(
            @Param("idUsuario") Integer idUsuario,
            Pageable pageable
    );

    List<DisputaPago> findByEstadoDisputaInOrderByFechaAperturaAsc(
            List<String> estados,
            Pageable pageable
    );
}
