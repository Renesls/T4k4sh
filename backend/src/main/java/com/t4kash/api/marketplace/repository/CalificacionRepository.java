package com.t4kash.api.marketplace.repository;

import com.t4kash.api.marketplace.entity.Calificacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface CalificacionRepository extends JpaRepository<Calificacion, Integer> {

    @Query("SELECT COALESCE(AVG(c.puntuacion), 0.0) FROM Calificacion c WHERE c.idCalificado = :usuarioId")
    Double obtenerPromedioReputacion(Integer usuarioId);

    @Query("SELECT COUNT(c) FROM Calificacion c WHERE c.idCalificado = :usuarioId")
    Long contarCalificaciones(Integer usuarioId);

    boolean existsByIdTrabajoAndIdCalificador(Integer idTrabajo, Integer idCalificador);
}
