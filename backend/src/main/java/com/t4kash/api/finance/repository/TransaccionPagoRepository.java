package com.t4kash.api.finance.repository;

import com.t4kash.api.finance.entity.TransaccionPago;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TransaccionPagoRepository extends JpaRepository<TransaccionPago, Long> {
    boolean existsByClaveIdempotencia(String claveIdempotencia);
    List<TransaccionPago> findTop30ByIdUsuarioOrderByFechaRegistroDesc(Integer idUsuario);
}
