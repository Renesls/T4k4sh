package com.t4kash.api.identity.repository;

import com.t4kash.api.identity.entity.VerificacionUsuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VerificacionUsuarioRepository
        extends JpaRepository<VerificacionUsuario, Integer> {
    Optional<VerificacionUsuario>
    findFirstByCorreoInstitucionalIgnoreCaseOrderByFechaSolicitudDesc(String correo);

    Optional<VerificacionUsuario>
    findFirstByCorreoInstitucionalIgnoreCaseAndTipoVerificacionOrderByFechaSolicitudDesc(
            String correo,
            String tipoVerificacion
    );
}
