package com.t4kash.api.marketplace.repository;

import com.t4kash.api.marketplace.entity.TrabajoAsignado;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TrabajoAsignadoRepository extends JpaRepository<TrabajoAsignado, Integer> {
    List<TrabajoAsignado> findAllByOrderByFechaInicioDesc();

    Optional<TrabajoAsignado> findByIdTarea(Integer idTarea);
}
