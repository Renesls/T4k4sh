package com.t4kash.api.identity.repository;

import com.t4kash.api.identity.entity.Universidad;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UniversidadRepository extends JpaRepository<Universidad, Integer> {
    List<Universidad> findAllByEstadoTrueOrderByNombreUniversidad();

    Optional<Universidad> findByIdUniversidadAndEstadoTrue(Integer idUniversidad);
}
