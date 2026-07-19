package com.t4kash.api.marketplace.dto;

import com.t4kash.api.marketplace.entity.Tarea;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TaskResponse(
        Integer idTarea,
        String titulo,
        String descripcion,
        BigDecimal presupuesto,
        LocalDateTime fechaPublicacion,
        LocalDateTime fechaLimitePostulacion,
        LocalDateTime fechaLimite,
        String estadoTarea,
        Integer idCategoria,
        Integer idCliente,
        String tipoOportunidad,
        String modalidad,
        String visibilidad
) {
    public static TaskResponse fromEntity(Tarea tarea) {
        return new TaskResponse(
                tarea.getIdTarea(),
                tarea.getTitulo(),
                tarea.getDescripcion(),
                tarea.getPresupuesto(),
                tarea.getFechaPublicacion(),
                tarea.getFechaLimitePostulacion(),
                tarea.getFechaLimite(),
                tarea.getEstadoTarea(),
                tarea.getIdCategoria(),
                tarea.getIdCliente(),
                tarea.getTipoOportunidad(),
                tarea.getModalidad(),
                tarea.getVisibilidad()
        );
    }
}
