package com.t4kash.api.identity.repository;

import com.t4kash.api.identity.entity.SesionUsuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;

public interface SesionUsuarioRepository extends JpaRepository<SesionUsuario, Integer> {
    Optional<SesionUsuario> findByTokenHashAndEstadoSesion(String tokenHash, String estadoSesion);

    List<SesionUsuario> findAllByIdUsuarioAndEstadoSesion(
            Integer idUsuario,
            String estadoSesion
    );
}
