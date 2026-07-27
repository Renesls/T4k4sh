package com.t4kash.api.marketplace.repository;

import com.t4kash.api.marketplace.entity.ArchivoAdjunto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ArchivoAdjuntoRepository extends JpaRepository<ArchivoAdjunto, Integer> {
    List<ArchivoAdjunto> findByIdTareaAndEstadoArchivoOrderByFechaSubidaDesc(
            Integer idTarea,
            String estadoArchivo
    );

    List<ArchivoAdjunto> findByIdEntregaAndEstadoArchivoOrderByFechaSubidaDesc(
            Integer idEntrega,
            String estadoArchivo
    );

    List<ArchivoAdjunto> findByIdEntregaInAndEstadoArchivoOrderByFechaSubidaDesc(
            List<Integer> idEntregas,
            String estadoArchivo
    );
}
