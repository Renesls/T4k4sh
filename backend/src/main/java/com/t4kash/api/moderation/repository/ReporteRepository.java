package com.t4kash.api.moderation.repository;

import com.t4kash.api.moderation.entity.Reporte;
import org.springframework.data.domain.Pageable;
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
    List<Reporte> findByIdUsuarioReportaOrderByFechaReporteDesc(
            Integer idUsuarioReporta,
            Pageable pageable
    );

    List<Reporte> findAllByOrderByFechaReporteDesc();
    List<Reporte> findAllByOrderByFechaReporteDesc(Pageable pageable);

    long countByEstadoReporte(String estadoReporte);
}
