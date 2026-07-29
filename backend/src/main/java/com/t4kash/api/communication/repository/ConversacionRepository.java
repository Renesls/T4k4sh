package com.t4kash.api.communication.repository;

import com.t4kash.api.communication.entity.Conversacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ConversacionRepository extends JpaRepository<Conversacion, Integer> {
    Optional<Conversacion> findByIdTrabajo(Integer idTrabajo);

    @Query(
            value = """
            SELECT DISTINCT conversacion.*
            FROM conversaciones conversacion
            INNER JOIN tareas tarea
                    ON tarea.id_tarea = conversacion.id_tarea
            LEFT JOIN trabajos_asignados trabajo
                   ON trabajo.id_trabajo = conversacion.id_trabajo
            LEFT JOIN postulaciones postulacion
                   ON postulacion.id_postulacion = conversacion.id_postulacion
            WHERE tarea.id_cliente = :idUsuario
               OR trabajo.id_estudiante = :idUsuario
               OR (
                    trabajo.id_trabajo IS NULL
                    AND postulacion.id_estudiante = :idUsuario
               )
            ORDER BY COALESCE(
                    conversacion.fecha_ultimo_mensaje,
                    conversacion.fecha_creacion
            ) DESC
            """,
            nativeQuery = true
    )
    List<Conversacion> findVisibleToUser(
            @Param("idUsuario") Integer idUsuario
    );
}
