package com.t4kash.api.marketplace.repository;

import com.t4kash.api.marketplace.entity.Postulacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PostulacionRepository extends JpaRepository<Postulacion, Integer> {
    List<Postulacion> findByIdTareaOrderByFechaPostulacionDesc(Integer idTarea);

    List<Postulacion> findByIdTareaAndEstadoPostulacionAndIdPostulacionNot(
            Integer idTarea,
            String estadoPostulacion,
            Integer idPostulacion
    );

    Optional<Postulacion>
    findFirstByIdTareaAndIdEstudianteOrderByNumeroIntentoDesc(
            Integer idTarea,
            Integer idEstudiante
    );

    List<Postulacion>
    findByIdEstudianteOrderByFechaPostulacionDesc(Integer idEstudiante);

    List<Postulacion>
    findByIdEstudianteAndEstadoPostulacion(
            Integer idEstudiante,
            String estadoPostulacion
    );

    List<Postulacion>
    findByIdTareaAndEstadoPostulacion(
            Integer idTarea,
            String estadoPostulacion
    );
}
