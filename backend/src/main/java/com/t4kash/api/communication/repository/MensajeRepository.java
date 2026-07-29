package com.t4kash.api.communication.repository;

import com.t4kash.api.communication.entity.Mensaje;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface MensajeRepository extends JpaRepository<Mensaje, Integer> {
    List<Mensaje> findByIdConversacionOrderByFechaEnvioAsc(
            Integer idConversacion
    );

    Optional<Mensaje> findFirstByIdConversacionOrderByFechaEnvioDesc(
            Integer idConversacion
    );

    long countByIdConversacionAndIdUsuarioEmisorNotAndLeidoFalse(
            Integer idConversacion,
            Integer idUsuarioEmisor
    );

    @Modifying
    @Query("""
            UPDATE Mensaje mensaje
            SET mensaje.leido = true,
                mensaje.fechaLectura = :fechaLectura
            WHERE mensaje.idConversacion = :idConversacion
              AND mensaje.idUsuarioEmisor <> :idUsuario
              AND mensaje.leido = false
            """)
    int markConversationAsRead(
            @Param("idConversacion") Integer idConversacion,
            @Param("idUsuario") Integer idUsuario,
            @Param("fechaLectura") LocalDateTime fechaLectura
    );
}
