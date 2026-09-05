package com.t4kash.api.network.repository;

import com.t4kash.api.network.entity.Publicacion;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PublicacionRepository extends JpaRepository<Publicacion, Integer> {
    @Query(
            value = """
            SELECT publicacion.*
            FROM publicaciones publicacion
            INNER JOIN usuarios autor
                    ON autor.id_usuario = publicacion.id_usuario
                   AND autor.estado_usuario = 'ACTIVO'
            INNER JOIN usuarios lector
                    ON lector.id_usuario = :idUsuario
            WHERE publicacion.estado_publicacion = 'ACTIVA'
              AND NOT EXISTS (
                    SELECT 1
                    FROM publicaciones_ocultas oculta
                    WHERE oculta.id_publicacion = publicacion.id_publicacion
                      AND oculta.id_usuario = :idUsuario
              )
              AND NOT EXISTS (
                    SELECT 1
                    FROM bloqueos_usuarios bloqueo
                    WHERE (bloqueo.id_usuario_bloquea = :idUsuario
                           AND bloqueo.id_usuario_bloqueado = publicacion.id_usuario)
                       OR (bloqueo.id_usuario_bloquea = publicacion.id_usuario
                           AND bloqueo.id_usuario_bloqueado = :idUsuario)
              )
              AND (
                    publicacion.id_usuario = :idUsuario
                    OR publicacion.visibilidad = 'PUBLICA'
                    OR (
                        publicacion.visibilidad = 'UNIVERSIDAD'
                        AND lector.id_universidad IS NOT NULL
                        AND lector.id_universidad = autor.id_universidad
                    )
                    OR (
                        publicacion.visibilidad = 'CONEXIONES'
                        AND EXISTS (
                            SELECT 1
                            FROM conexiones_usuarios conexion
                            WHERE conexion.estado_conexion = 'ACEPTADA'
                              AND (
                                  (conexion.id_usuario_solicitante = :idUsuario
                                   AND conexion.id_usuario_receptor = publicacion.id_usuario)
                                  OR
                                  (conexion.id_usuario_receptor = :idUsuario
                                   AND conexion.id_usuario_solicitante = publicacion.id_usuario)
                              )
                        )
                    )
              )
              AND (
                    :alcance = 'PARA_TI'
                    OR (
                        :alcance = 'CONEXIONES'
                        AND (
                            publicacion.id_usuario = :idUsuario
                            OR EXISTS (
                                SELECT 1
                                FROM conexiones_usuarios conexion
                                WHERE conexion.estado_conexion = 'ACEPTADA'
                                  AND (
                                      (conexion.id_usuario_solicitante = :idUsuario
                                       AND conexion.id_usuario_receptor = publicacion.id_usuario)
                                      OR
                                      (conexion.id_usuario_receptor = :idUsuario
                                       AND conexion.id_usuario_solicitante = publicacion.id_usuario)
                                  )
                            )
                        )
                    )
                    OR (
                        :alcance = 'UNIVERSIDAD'
                        AND lector.id_universidad IS NOT NULL
                        AND lector.id_universidad = autor.id_universidad
                    )
              )
            ORDER BY publicacion.fecha_publicacion DESC,
                     publicacion.id_publicacion DESC
            """,
            nativeQuery = true
    )
    List<Publicacion> findFeed(
            @Param("idUsuario") Integer idUsuario,
            @Param("alcance") String alcance,
            Pageable pageable
    );

    @Query(
            value = """
            SELECT publicacion.*
            FROM publicaciones publicacion
            INNER JOIN usuarios autor
                    ON autor.id_usuario = publicacion.id_usuario
                   AND autor.estado_usuario = 'ACTIVO'
            INNER JOIN usuarios lector
                    ON lector.id_usuario = :idUsuario
            WHERE publicacion.id_publicacion = :idPublicacion
              AND publicacion.estado_publicacion = 'ACTIVA'
              AND NOT EXISTS (
                    SELECT 1
                    FROM publicaciones_ocultas oculta
                    WHERE oculta.id_publicacion = publicacion.id_publicacion
                      AND oculta.id_usuario = :idUsuario
              )
              AND NOT EXISTS (
                    SELECT 1
                    FROM bloqueos_usuarios bloqueo
                    WHERE (bloqueo.id_usuario_bloquea = :idUsuario
                           AND bloqueo.id_usuario_bloqueado = publicacion.id_usuario)
                       OR (bloqueo.id_usuario_bloquea = publicacion.id_usuario
                           AND bloqueo.id_usuario_bloqueado = :idUsuario)
              )
              AND (
                    publicacion.id_usuario = :idUsuario
                    OR publicacion.visibilidad = 'PUBLICA'
                    OR (
                        publicacion.visibilidad = 'UNIVERSIDAD'
                        AND lector.id_universidad IS NOT NULL
                        AND lector.id_universidad = autor.id_universidad
                    )
                    OR (
                        publicacion.visibilidad = 'CONEXIONES'
                        AND EXISTS (
                            SELECT 1
                            FROM conexiones_usuarios conexion
                            WHERE conexion.estado_conexion = 'ACEPTADA'
                              AND (
                                  (conexion.id_usuario_solicitante = :idUsuario
                                   AND conexion.id_usuario_receptor = publicacion.id_usuario)
                                  OR
                                  (conexion.id_usuario_receptor = :idUsuario
                                   AND conexion.id_usuario_solicitante = publicacion.id_usuario)
                              )
                        )
                    )
              )
            """,
            nativeQuery = true
    )
    Optional<Publicacion> findVisibleById(
            @Param("idUsuario") Integer idUsuario,
            @Param("idPublicacion") Integer idPublicacion
    );

    @Query(
            value = """
            SELECT publicacion.*
            FROM publicaciones publicacion
            INNER JOIN publicaciones_guardadas guardada
                    ON guardada.id_publicacion = publicacion.id_publicacion
                   AND guardada.id_usuario = :idUsuario
            INNER JOIN usuarios autor
                    ON autor.id_usuario = publicacion.id_usuario
                   AND autor.estado_usuario = 'ACTIVO'
            INNER JOIN usuarios lector
                    ON lector.id_usuario = :idUsuario
            WHERE publicacion.estado_publicacion = 'ACTIVA'
              AND NOT EXISTS (
                    SELECT 1
                    FROM bloqueos_usuarios bloqueo
                    WHERE (bloqueo.id_usuario_bloquea = :idUsuario
                           AND bloqueo.id_usuario_bloqueado = publicacion.id_usuario)
                       OR (bloqueo.id_usuario_bloquea = publicacion.id_usuario
                           AND bloqueo.id_usuario_bloqueado = :idUsuario)
              )
              AND (
                    publicacion.id_usuario = :idUsuario
                    OR publicacion.visibilidad = 'PUBLICA'
                    OR (
                        publicacion.visibilidad = 'UNIVERSIDAD'
                        AND lector.id_universidad IS NOT NULL
                        AND lector.id_universidad = autor.id_universidad
                    )
                    OR (
                        publicacion.visibilidad = 'CONEXIONES'
                        AND EXISTS (
                            SELECT 1
                            FROM conexiones_usuarios conexion
                            WHERE conexion.estado_conexion = 'ACEPTADA'
                              AND (
                                  (conexion.id_usuario_solicitante = :idUsuario
                                   AND conexion.id_usuario_receptor = publicacion.id_usuario)
                                  OR
                                  (conexion.id_usuario_receptor = :idUsuario
                                   AND conexion.id_usuario_solicitante = publicacion.id_usuario)
                              )
                        )
                    )
              )
            ORDER BY guardada.fecha_guardado DESC
            """,
            nativeQuery = true
    )
    List<Publicacion> findSaved(
            @Param("idUsuario") Integer idUsuario,
            Pageable pageable
    );

    @Query("""
            SELECT publicacion.idPublicacionOrigen AS idPublicacion,
                   COUNT(publicacion) AS total
            FROM Publicacion publicacion
            WHERE publicacion.idPublicacionOrigen IN :ids
              AND publicacion.estadoPublicacion = 'ACTIVA'
            GROUP BY publicacion.idPublicacionOrigen
            """)
    List<PublicationCountProjection> countSharesByPublicationIds(
            @Param("ids") Collection<Integer> ids
    );
}
