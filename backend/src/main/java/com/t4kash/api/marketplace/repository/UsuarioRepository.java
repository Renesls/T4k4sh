package com.t4kash.api.marketplace.repository;

import com.t4kash.api.marketplace.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {
}
