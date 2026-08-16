package com.t4kash.api.network.repository;

import com.t4kash.api.network.entity.ReaccionPublicacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ReaccionPublicacionRepository
        extends JpaRepository<ReaccionPublicacion, Integer> {
    Optional<ReaccionPublicacion> findByIdPublicacionAndIdUsuario(
            Integer idPublicacion,
            Integer idUsuario
    );

    List<ReaccionPublicacion> findByIdUsuarioAndIdPublicacionIn(
            Integer idUsuario,
            Collection<Integer> ids
    );

    @Query("""
            SELECT reaccion.idPublicacion AS idPublicacion,
                   reaccion.tipoReaccion AS tipoReaccion,
                   COUNT(reaccion) AS total
            FROM ReaccionPublicacion reaccion
            WHERE reaccion.idPublicacion IN :ids
            GROUP BY reaccion.idPublicacion, reaccion.tipoReaccion
            """)
    List<ReactionCountProjection> countByPublicationIds(
            @Param("ids") Collection<Integer> ids
    );

    void deleteByIdPublicacionAndIdUsuario(
            Integer idPublicacion,
            Integer idUsuario
    );
}
