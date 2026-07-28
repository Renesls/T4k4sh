package com.t4kash.api.identity.repository;

import com.t4kash.api.marketplace.entity.Usuario;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UsuarioRolRepository extends Repository<Usuario, Integer> {
    @Query(
            value = """
                    SELECT r.nombre_rol
                    FROM roles r
                    INNER JOIN usuario_roles ur ON ur.id_rol = r.id_rol
                    WHERE ur.id_usuario = :idUsuario
                    ORDER BY r.nombre_rol
                    """,
            nativeQuery = true
    )
    List<String> findRoleNames(@Param("idUsuario") Integer idUsuario);

    @Modifying
    @Query(
            value = """
                    INSERT INTO usuario_roles (id_usuario, id_rol)
                    SELECT :idUsuario, id_rol
                    FROM roles
                    WHERE nombre_rol IN ('CLIENTE', 'ESTUDIANTE')
                    ON CONFLICT (id_usuario, id_rol) DO NOTHING
                    """,
            nativeQuery = true
    )
    int assignMarketplaceRoles(@Param("idUsuario") Integer idUsuario);
}
