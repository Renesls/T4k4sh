package com.t4kash.api.identity.repository;

import com.t4kash.api.identity.entity.TokenRecuperacionPassword;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TokenRecuperacionPasswordRepository
        extends JpaRepository<TokenRecuperacionPassword, Integer> {
    Optional<TokenRecuperacionPassword>
    findFirstByIdUsuarioAndUsadoFalseOrderByFechaCreacionDesc(Integer idUsuario);

    List<TokenRecuperacionPassword> findAllByIdUsuarioAndUsadoFalse(Integer idUsuario);
}
