package com.t4kash.api.communication.service;

import com.t4kash.api.communication.dto.NotificationResponse;
import com.t4kash.api.config.PaginationSupport;
import com.t4kash.api.communication.entity.Notificacion;
import com.t4kash.api.communication.repository.NotificacionRepository;
import com.t4kash.api.exception.ForbiddenOperationException;
import com.t4kash.api.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class NotificationService {
    private final NotificacionRepository notificationRepository;

    public NotificationService(NotificacionRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Transactional
    public void create(Integer userId, String title, String message) {
        Notificacion notification = new Notificacion();
        notification.setIdUsuario(userId);
        notification.setTitulo(limit(title, 150));
        notification.setMensaje(limit(message, 500));
        notification.setLeida(false);
        notification.setFechaCreacion(LocalDateTime.now());
        notificationRepository.save(notification);
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> listMine(Integer userId) {
        return listMine(userId, 0, PaginationSupport.DEFAULT_SIZE);
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> listMine(Integer userId, int page, int size) {
        return notificationRepository
                .findByIdUsuarioOrderByFechaCreacionDesc(
                        userId,
                        PaginationSupport.page(page, size)
                )
                .stream()
                .map(NotificationResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public long countUnread(Integer userId) {
        return notificationRepository.countByIdUsuarioAndLeidaFalse(userId);
    }

    @Transactional
    public NotificationResponse markRead(Integer userId, Integer notificationId) {
        Notificacion notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "La notificacion indicada no existe."
                ));
        if (!notification.getIdUsuario().equals(userId)) {
            throw new ForbiddenOperationException(
                    "No puedes modificar notificaciones de otra cuenta."
            );
        }
        if (!notification.isLeida()) {
            notification.setLeida(true);
            notification = notificationRepository.save(notification);
        }
        return NotificationResponse.fromEntity(notification);
    }

    @Transactional
    public void markAllRead(Integer userId) {
        notificationRepository.markAllAsRead(userId);
    }

    private String limit(String value, int maximum) {
        String clean = value == null ? "" : value.trim();
        return clean.length() <= maximum
                ? clean
                : clean.substring(0, maximum);
    }
}
