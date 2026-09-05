package com.t4kash.api.network.repository;

import com.t4kash.api.network.entity.PublicacionGuardada;
import com.t4kash.api.network.entity.PublicacionGuardadaId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface PublicacionGuardadaRepository
        extends JpaRepository<PublicacionGuardada, PublicacionGuardadaId> {
    List<PublicacionGuardada> findByIdUsuarioAndIdPublicacionIn(
            Integer idUsuario,
            Collection<Integer> ids
    );

    void deleteByIdPublicacionAndIdUsuario(
            Integer idPublicacion,
            Integer idUsuario
    );
}
