package com.t4kash.api.communication.controller;

import com.t4kash.api.communication.dto.ConversationResponse;
import com.t4kash.api.communication.dto.CreateMessageRequest;
import com.t4kash.api.communication.dto.MessageResponse;
import com.t4kash.api.communication.dto.NotificationResponse;
import com.t4kash.api.communication.service.ConversationService;
import com.t4kash.api.communication.service.NotificationService;
import com.t4kash.api.identity.dto.AuthenticatedUserResponse;
import com.t4kash.api.identity.web.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
@Tag(
        name = "Comunicacion",
        description = "Conversaciones, mensajes y notificaciones"
)
@SecurityRequirement(name = "bearerAuth")
public class CommunicationController {
    private final ConversationService conversationService;
    private final NotificationService notificationService;

    public CommunicationController(
            ConversationService conversationService,
            NotificationService notificationService
    ) {
        this.conversationService = conversationService;
        this.notificationService = notificationService;
    }

    @GetMapping("/conversations")
    @Operation(summary = "Listar mis conversaciones")
    public List<ConversationResponse> listConversations(
            @CurrentUser AuthenticatedUserResponse user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        return conversationService.listMine(user.idUsuario(), page, size);
    }

    @GetMapping("/conversations/{conversationId}/messages")
    @Operation(summary = "Listar mensajes de una conversacion")
    public List<MessageResponse> listMessages(
            @CurrentUser AuthenticatedUserResponse user,
            @PathVariable Integer conversationId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size
    ) {
        return conversationService.listMessages(
                user.idUsuario(),
                conversationId,
                page,
                size
        );
    }

    @PostMapping("/conversations/{conversationId}/messages")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Enviar un mensaje")
    public MessageResponse sendMessage(
            @CurrentUser AuthenticatedUserResponse user,
            @PathVariable Integer conversationId,
            @Valid @RequestBody CreateMessageRequest request
    ) {
        return conversationService.sendMessage(
                user.idUsuario(),
                conversationId,
                request
        );
    }

    @PostMapping("/conversations/{conversationId}/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Marcar una conversacion como leida")
    public void markConversationRead(
            @CurrentUser AuthenticatedUserResponse user,
            @PathVariable Integer conversationId
    ) {
        conversationService.markRead(user.idUsuario(), conversationId);
    }

    @GetMapping("/notifications")
    @Operation(summary = "Listar mis notificaciones")
    public List<NotificationResponse> listNotifications(
            @CurrentUser AuthenticatedUserResponse user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        return notificationService.listMine(user.idUsuario(), page, size);
    }

    @PostMapping("/notifications/{notificationId}/read")
    @Operation(summary = "Marcar una notificacion como leida")
    public NotificationResponse markNotificationRead(
            @CurrentUser AuthenticatedUserResponse user,
            @PathVariable Integer notificationId
    ) {
        return notificationService.markRead(
                user.idUsuario(),
                notificationId
        );
    }

    @PostMapping("/notifications/read-all")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Marcar todas las notificaciones como leidas")
    public void markAllNotificationsRead(
            @CurrentUser AuthenticatedUserResponse user
    ) {
        notificationService.markAllRead(user.idUsuario());
    }
}
