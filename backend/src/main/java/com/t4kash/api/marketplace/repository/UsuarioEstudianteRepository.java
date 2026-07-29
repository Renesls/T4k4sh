package com.t4kash.api.marketplace.repository;

import com.t4kash.api.marketplace.entity.UsuarioEstudiante;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.util.Optional;

public interface UsuarioEstudianteRepository extends JpaRepository<UsuarioEstudiante, Integer> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT estudiante FROM UsuarioEstudiante estudiante WHERE estudiante.idUsuario = :id")
    Optional<UsuarioEstudiante> findByIdForUpdate(@Param("id") Integer id);
}
