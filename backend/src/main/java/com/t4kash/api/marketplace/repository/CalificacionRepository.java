package com.t4kash.api.marketplace.repository;

import com.t4kash.api.marketplace.entity.Calificacion;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface CalificacionRepository extends JpaRepository<Calificacion, Integer> {

    @Query("SELECT AVG(c.puntuacion) FROM Calificacion c WHERE c.idCalificado = :usuarioId")
    Double obtenerPromedioReputacion(Integer usuarioId);

    @Query("SELECT COUNT(c) FROM Calificacion c WHERE c.idCalificado = :usuarioId")
    long contarCalificaciones(Integer usuarioId);

    boolean existsByIdTrabajoAndIdCalificador(Integer idTrabajo, Integer idCalificador);

    List<Calificacion> findByIdTrabajoOrderByFechaCalificacionDesc(Integer idTrabajo);

    List<Calificacion> findTop5ByIdCalificadoOrderByFechaCalificacionDesc(Integer idCalificado);
}
