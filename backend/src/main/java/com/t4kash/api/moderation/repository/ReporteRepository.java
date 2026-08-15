package com.t4kash.api.moderation.repository;

import com.t4kash.api.moderation.entity.Reporte;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReporteRepository extends JpaRepository<Reporte, Integer> {
    boolean existsByIdUsuarioReportaAndIdTareaAndEstadoReporte(
            Integer idUsuarioReporta,
            Integer idTarea,
            String estadoReporte
    );

    List<Reporte> findByIdUsuarioReportaOrderByFechaReporteDesc(
            Integer idUsuarioReporta
    );

    List<Reporte> findAllByOrderByFechaReporteDesc();

    long countByEstadoReporte(String estadoReporte);
}
