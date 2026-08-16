package com.t4kash.api.network.repository;

import com.t4kash.api.network.entity.ComentarioPublicacion;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface ComentarioPublicacionRepository
        extends JpaRepository<ComentarioPublicacion, Integer> {
    @Query(
            value = """
            SELECT comentario.*
            FROM comentarios_publicacion comentario
            INNER JOIN usuarios autor
                    ON autor.id_usuario = comentario.id_usuario
                   AND autor.estado_usuario = 'ACTIVO'
            WHERE comentario.id_publicacion = :idPublicacion
              AND comentario.estado_comentario = 'ACTIVO'
              AND NOT EXISTS (
                    SELECT 1
                    FROM bloqueos_usuarios bloqueo
                    WHERE (bloqueo.id_usuario_bloquea = :idUsuario
                           AND bloqueo.id_usuario_bloqueado = comentario.id_usuario)
                       OR (bloqueo.id_usuario_bloquea = comentario.id_usuario
                           AND bloqueo.id_usuario_bloqueado = :idUsuario)
              )
            ORDER BY comentario.fecha_comentario,
                     comentario.id_comentario_publicacion
            """,
            nativeQuery = true
    )
    List<ComentarioPublicacion> findVisibleComments(
            @Param("idUsuario") Integer idUsuario,
            @Param("idPublicacion") Integer idPublicacion,
            Pageable pageable
    );

    @Query("""
            SELECT comentario.idPublicacion AS idPublicacion,
                   COUNT(comentario) AS total
            FROM ComentarioPublicacion comentario
            WHERE comentario.idPublicacion IN :ids
              AND comentario.estadoComentario = 'ACTIVO'
            GROUP BY comentario.idPublicacion
            """)
    List<PublicationCountProjection> countByPublicationIds(
            @Param("ids") Collection<Integer> ids
    );
}
