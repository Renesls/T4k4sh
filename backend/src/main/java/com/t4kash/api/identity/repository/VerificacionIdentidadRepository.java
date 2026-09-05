package com.t4kash.api.identity.repository;

import com.t4kash.api.identity.entity.VerificacionIdentidad;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface VerificacionIdentidadRepository
        extends JpaRepository<VerificacionIdentidad, Integer> {
    Optional<VerificacionIdentidad> findFirstByIdUsuarioOrderByFechaInicioDesc(
            Integer idUsuario
    );

    Optional<VerificacionIdentidad> findFirstByIdUsuarioAndEstadoVerificacionInOrderByFechaInicioDesc(
            Integer idUsuario,
            Collection<String> estados
    );

    Optional<VerificacionIdentidad> findByIdSesionProveedor(UUID idSesionProveedor);

    Optional<VerificacionIdentidad> findFirstByHuellaDocumentoAndEstadoVerificacion(
            String huellaDocumento,
            String estadoVerificacion
    );
}
