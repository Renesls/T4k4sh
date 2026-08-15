package com.t4kash.api.marketplace.repository;

import com.t4kash.api.marketplace.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {
    Optional<Usuario> findByCorreoIgnoreCase(String correo);

    boolean existsByCorreoIgnoreCase(String correo);

    boolean existsByNombreUsuarioIgnoreCase(String nombreUsuario);

    boolean existsByNombreUsuarioIgnoreCaseAndIdUsuarioNot(
            String nombreUsuario,
            Integer idUsuario
    );

    Optional<Usuario> findByNombreUsuarioIgnoreCaseAndEstadoUsuarioIgnoreCase(
            String nombreUsuario,
            String estadoUsuario
    );
}
