package com.t4kash.api.finance.repository;

import com.t4kash.api.finance.entity.Pago;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface PagoRepository extends JpaRepository<Pago, Integer> {
    Optional<Pago> findByIdTrabajo(Integer idTrabajo);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT pago FROM Pago pago WHERE pago.idTrabajo = :idTrabajo")
    Optional<Pago> findByIdTrabajoForUpdate(@Param("idTrabajo") Integer idTrabajo);
    Optional<Pago> findByReferenciaComercio(String referenciaComercio);
    Optional<Pago> findByReferenciaProveedor(String referenciaProveedor);

    @Query("""
            SELECT pago FROM Pago pago
            WHERE pago.idCliente = :idUsuario OR pago.idEstudiante = :idUsuario
            ORDER BY pago.fechaActualizacion DESC
            """)
    List<Pago> findVisibleToUser(@Param("idUsuario") Integer idUsuario);

    @Query("""
            SELECT pago FROM Pago pago
            WHERE pago.idCliente = :idUsuario OR pago.idEstudiante = :idUsuario
            ORDER BY pago.fechaActualizacion DESC
            """)
    List<Pago> findVisibleToUser(
            @Param("idUsuario") Integer idUsuario,
            Pageable pageable
    );

    @Query("""
            SELECT COALESCE(SUM(pago.montoEstudiante), 0) FROM Pago pago
            WHERE pago.idEstudiante = :idUsuario
              AND pago.estadoPago = :estado
            """)
    BigDecimal sumStudentAmountByStatus(
            @Param("idUsuario") Integer idUsuario,
            @Param("estado") String estado
    );

    @Query("""
            SELECT COALESCE(SUM(pago.montoEstudiante), 0) FROM Pago pago
            WHERE pago.idEstudiante = :idUsuario
              AND pago.estadoPago IN :estados
            """)
    BigDecimal sumStudentAmountByStatuses(
            @Param("idUsuario") Integer idUsuario,
            @Param("estados") List<String> estados
    );
}
