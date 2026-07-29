package com.t4kash.api.communication.repository;

import com.t4kash.api.communication.entity.Notificacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificacionRepository extends JpaRepository<Notificacion, Integer> {
    List<Notificacion> findByIdUsuarioOrderByFechaCreacionDesc(Integer idUsuario);

    long countByIdUsuarioAndLeidaFalse(Integer idUsuario);
}
