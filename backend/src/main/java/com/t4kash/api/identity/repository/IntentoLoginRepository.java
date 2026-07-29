package com.t4kash.api.identity.repository;

import com.t4kash.api.identity.entity.IntentoLogin;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface IntentoLoginRepository extends JpaRepository<IntentoLogin, Integer> {
    long countByCorreoIgnoreCaseAndExitosoFalseAndFechaIntentoAfter(
            String correo,
            LocalDateTime fecha
    );

    Optional<IntentoLogin>
    findFirstByCorreoIgnoreCaseAndExitosoTrueOrderByFechaIntentoDesc(String correo);

    Optional<IntentoLogin>
    findFirstByCorreoIgnoreCaseAndExitosoFalseOrderByFechaIntentoDesc(String correo);
}
