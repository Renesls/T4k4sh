package com.t4kash.api.communication.dto;

import com.t4kash.api.communication.entity.Notificacion;

import java.time.LocalDateTime;

public record NotificationResponse(
        Integer idNotificacion,
        String titulo,
        String mensaje,
        boolean leida,
        LocalDateTime fechaCreacion
) {
    public static NotificationResponse fromEntity(Notificacion notification) {
        return new NotificationResponse(
                notification.getIdNotificacion(),
                notification.getTitulo(),
                notification.getMensaje(),
                notification.isLeida(),
                notification.getFechaCreacion()
        );
    }
}
