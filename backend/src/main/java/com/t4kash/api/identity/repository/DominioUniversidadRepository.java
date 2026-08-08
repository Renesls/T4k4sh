package com.t4kash.api.identity.repository;

import com.t4kash.api.identity.entity.DominioUniversidad;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DominioUniversidadRepository
        extends JpaRepository<DominioUniversidad, Integer> {
    Optional<DominioUniversidad> findByDominioCorreoIgnoreCaseAndEstadoTrue(
            String dominioCorreo
    );
}
