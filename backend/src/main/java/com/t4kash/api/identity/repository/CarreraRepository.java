package com.t4kash.api.identity.repository;

import com.t4kash.api.identity.entity.Carrera;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CarreraRepository extends JpaRepository<Carrera, Integer> {
    List<Carrera> findAllByIdUniversidadOrderByNombreCarrera(Integer idUniversidad);

    Optional<Carrera> findByIdCarreraAndIdUniversidad(
            Integer idCarrera,
            Integer idUniversidad
    );
}
