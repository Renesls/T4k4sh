package com.t4kash.api.identity.repository;

import com.t4kash.api.identity.entity.HistorialNombreUsuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface HistorialNombreUsuarioRepository
        extends JpaRepository<HistorialNombreUsuario, Integer> {
    Optional<HistorialNombreUsuario> findFirstByIdUsuarioOrderByFechaCambioDesc(
            Integer idUsuario
    );
}
