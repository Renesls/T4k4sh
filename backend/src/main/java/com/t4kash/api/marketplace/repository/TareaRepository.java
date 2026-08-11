package com.t4kash.api.marketplace.repository;

import com.t4kash.api.marketplace.entity.Tarea;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface TareaRepository extends JpaRepository<Tarea, Integer> {
    List<Tarea> findAllByOrderByFechaPublicacionDesc();
    Page<Tarea> findAllByOrderByFechaPublicacionDesc(Pageable pageable);

    @Modifying
    @Query("""
            UPDATE Tarea tarea
            SET tarea.estadoTarea = 'CERRADA'
            WHERE tarea.estadoTarea = 'PUBLICADA'
              AND tarea.fechaLimitePostulacion IS NOT NULL
              AND tarea.fechaLimitePostulacion <= :now
            """)
    int closeExpiredPublishedTasks(@Param("now") LocalDateTime now);

    @Query("""
            SELECT tarea
            FROM Tarea tarea
            WHERE UPPER(tarea.tipoOportunidad) = UPPER(:tipoOportunidad)
              AND UPPER(tarea.estadoTarea) = UPPER(:estadoTarea)
              AND tarea.latitud BETWEEN :latitudMinima AND :latitudMaxima
              AND tarea.longitud BETWEEN :longitudMinima AND :longitudMaxima
            ORDER BY tarea.fechaPublicacion DESC
            """)
    List<Tarea> findQuickTasksWithinBounds(
            @Param("tipoOportunidad") String tipoOportunidad,
            @Param("estadoTarea") String estadoTarea,
            @Param("latitudMinima") BigDecimal latitudMinima,
            @Param("latitudMaxima") BigDecimal latitudMaxima,
            @Param("longitudMinima") BigDecimal longitudMinima,
            @Param("longitudMaxima") BigDecimal longitudMaxima
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT tarea FROM Tarea tarea WHERE tarea.idTarea = :idTarea")
    Optional<Tarea> findByIdForUpdate(@Param("idTarea") Integer idTarea);

    long countByEstadoTareaIgnoreCase(String estadoTarea);

    long countByIdCliente(Integer idCliente);
}
