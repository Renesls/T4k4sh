package com.t4kash.api.marketplace.repository;

import com.t4kash.api.marketplace.entity.Evaluacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface EvaluacionRepository extends JpaRepository<Evaluacion, Long> {

    @Query("SELECT COALESCE(AVG(e.calificacion), 0.0) FROM Evaluacion e WHERE e.evaluado.idUsuario = :usuarioId")
    Double obtenerPromedioReputacion(Long usuarioId);

    @Query("SELECT COUNT(e) FROM Evaluacion e WHERE e.evaluado.idUsuario = :usuarioId")
    Long contarEvaluaciones(Long usuarioId);

    boolean existsByTrabajoAsignadoIdAndEvaluadorIdUsuario(Long trabajoAsignadoId, Long evaluadorId);
}