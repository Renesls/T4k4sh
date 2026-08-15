package com.t4kash.api.identity.repository;

import com.t4kash.api.identity.entity.VerificacionUsuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;

public interface VerificacionUsuarioRepository
        extends JpaRepository<VerificacionUsuario, Integer> {
    Optional<VerificacionUsuario>
    findFirstByCorreoInstitucionalIgnoreCaseOrderByFechaSolicitudDesc(String correo);

    Optional<VerificacionUsuario>
    findFirstByCorreoInstitucionalIgnoreCaseAndTipoVerificacionOrderByFechaSolicitudDesc(
            String correo,
            String tipoVerificacion
    );

    Optional<VerificacionUsuario>
    findFirstByIdUsuarioAndTipoVerificacionOrderByFechaSolicitudDesc(
            Integer idUsuario,
            String tipoVerificacion
    );

    List<VerificacionUsuario>
    findByTipoVerificacionAndEstadoVerificacionOrderByFechaSolicitudAsc(
            String tipoVerificacion,
            String estadoVerificacion
    );

    long countByTipoVerificacionAndEstadoVerificacion(
            String tipoVerificacion,
            String estadoVerificacion
    );
}
