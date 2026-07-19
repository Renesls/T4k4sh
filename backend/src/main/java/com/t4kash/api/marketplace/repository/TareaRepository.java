package com.t4kash.api.marketplace.repository;

import com.t4kash.api.marketplace.entity.Tarea;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TareaRepository extends JpaRepository<Tarea, Integer> {
    List<Tarea> findAllByOrderByFechaPublicacionDesc();
}
