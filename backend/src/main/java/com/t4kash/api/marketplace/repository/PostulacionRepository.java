package com.t4kash.api.marketplace.repository;

import com.t4kash.api.marketplace.entity.Postulacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PostulacionRepository extends JpaRepository<Postulacion, Integer> {
    List<Postulacion> findByIdTareaOrderByFechaPostulacionDesc(Integer idTarea);

    Optional<Postulacion> findByIdTareaAndIdEstudiante(Integer idTarea, Integer idEstudiante);
}
