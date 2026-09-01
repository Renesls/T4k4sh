package com.t4kash.api.finance.repository;

import com.t4kash.api.finance.entity.DesembolsoPago;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface DesembolsoPagoRepository extends JpaRepository<DesembolsoPago, Integer> {
    Optional<DesembolsoPago> findByClaveIdempotencia(String claveIdempotencia);
    List<DesembolsoPago> findByIdEstudianteOrderByFechaCreacionDesc(
            Integer idEstudiante,
            Pageable pageable
    );
}
