package com.t4kash.api.communication.repository;

import com.t4kash.api.communication.entity.Notificacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface NotificacionRepository extends JpaRepository<Notificacion, Integer> {
    List<Notificacion> findByIdUsuarioOrderByFechaCreacionDesc(Integer idUsuario);
    List<Notificacion> findByIdUsuarioOrderByFechaCreacionDesc(
            Integer idUsuario,
            Pageable pageable
    );

    long countByIdUsuarioAndLeidaFalse(Integer idUsuario);

    @Modifying
    @Query("""
            UPDATE Notificacion notificacion
            SET notificacion.leida = true
            WHERE notificacion.idUsuario = :idUsuario
              AND notificacion.leida = false
            """)
    int markAllAsRead(@Param("idUsuario") Integer idUsuario);
}
