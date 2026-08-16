package com.t4kash.api.finance.repository;

import com.t4kash.api.finance.entity.ReembolsoPago;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ReembolsoPagoRepository extends JpaRepository<ReembolsoPago, Integer> {
    Optional<ReembolsoPago> findByClaveIdempotencia(String claveIdempotencia);

    @Query(value = """
            SELECT reembolso.*
            FROM reembolsos_pago reembolso
            INNER JOIN pagos pago ON pago.id_pago = reembolso.id_pago
            WHERE pago.id_cliente = :idUsuario OR pago.id_estudiante = :idUsuario
            ORDER BY reembolso.fecha_solicitud DESC
            """, nativeQuery = true)
    List<ReembolsoPago> findVisibleToUser(
            @Param("idUsuario") Integer idUsuario,
            Pageable pageable
    );
}
