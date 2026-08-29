package com.t4kash.api.communication.service;

import com.t4kash.api.communication.dto.NotificationResponse;
import com.t4kash.api.communication.entity.Notificacion;
import com.t4kash.api.communication.repository.NotificacionRepository;
import com.t4kash.api.exception.ForbiddenOperationException;
import com.t4kash.api.exception.ResourceNotFoundException;
import com.t4kash.api.marketplace.entity.Usuario;
import com.t4kash.api.marketplace.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class NotificationService {
    private final NotificacionRepository notificationRepository;
    private final UsuarioRepository usuarioRepository;
    private final FcmService fcmService;

    public NotificationService(
            NotificacionRepository notificationRepository,
            UsuarioRepository usuarioRepository,
            FcmService fcmService
    ) {
        this.notificationRepository = notificationRepository;
        this.usuarioRepository = usuarioRepository;
        this.fcmService = fcmService;
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

        Usuario usuarioDestino = usuarioRepository.findById(userId).orElse(null);
        String token = usuarioDestino == null ? null : usuarioDestino.getFcmToken();
        if (token != null && !token.isBlank()) {
            fcmService.sendPushNotification(token, title, message);
        }
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> listMine(Integer userId) {
        return notificationRepository
                .findByIdUsuarioOrderByFechaCreacionDesc(userId)
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
        List<Notificacion> notifications =
                notificationRepository.findByIdUsuarioOrderByFechaCreacionDesc(userId);
        notifications.stream()
                .filter(item -> !item.isLeida())
                .forEach(item -> item.setLeida(true));
        notificationRepository.saveAll(notifications);
    }

    private String limit(String value, int maximum) {
        String clean = value == null ? "" : value.trim();
        return clean.length() <= maximum
                ? clean
                : clean.substring(0, maximum);
    }
}