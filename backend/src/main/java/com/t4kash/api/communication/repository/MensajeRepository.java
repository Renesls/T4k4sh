package com.t4kash.api.communication.repository;

import com.t4kash.api.communication.entity.Mensaje;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    Page<Mensaje> findByIdConversacionOrderByFechaEnvioDesc(
            Integer idConversacion,
            Pageable pageable
    );

    Optional<Mensaje> findFirstByIdConversacionOrderByFechaEnvioDesc(
            Integer idConversacion
    );

    long countByIdConversacionAndIdUsuarioEmisorNotAndLeidoFalse(
            Integer idConversacion,
            Integer idUsuarioEmisor
    );

    @Query(
            value = """
            SELECT DISTINCT ON (mensaje.id_conversacion) mensaje.*
            FROM mensajes mensaje
            WHERE mensaje.id_conversacion IN (:conversationIds)
            ORDER BY mensaje.id_conversacion, mensaje.fecha_envio DESC
            """,
            nativeQuery = true
    )
    List<Mensaje> findLatestByConversationIds(
            @Param("conversationIds") List<Integer> conversationIds
    );

    @Query("""
            SELECT mensaje.idConversacion AS idConversacion,
                   COUNT(mensaje) AS total
            FROM Mensaje mensaje
            WHERE mensaje.idConversacion IN :conversationIds
              AND mensaje.idUsuarioEmisor <> :currentUserId
              AND mensaje.leido = false
            GROUP BY mensaje.idConversacion
            """)
    List<UnreadCountProjection> countUnreadByConversationIds(
            @Param("conversationIds") List<Integer> conversationIds,
            @Param("currentUserId") Integer currentUserId
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
