package com.t4kash.api.marketplace.repository;

import com.t4kash.api.marketplace.entity.CategoriaTarea;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CategoriaTareaRepository extends JpaRepository<CategoriaTarea, Integer> {
    List<CategoriaTarea> findByEstadoTrueOrderByNombreCategoriaAsc();
}
