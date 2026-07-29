package com.t4kash.api.communication.controller;

import com.t4kash.api.communication.dto.ConversationResponse;
import com.t4kash.api.communication.dto.CreateMessageRequest;
import com.t4kash.api.communication.dto.MessageResponse;
import com.t4kash.api.communication.dto.NotificationResponse;
import com.t4kash.api.communication.service.ConversationService;
import com.t4kash.api.communication.service.NotificationService;
import com.t4kash.api.identity.dto.AuthenticatedUserResponse;
import com.t4kash.api.identity.service.AuthenticatedUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
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
    private final AuthenticatedUserService authenticatedUserService;
    private final ConversationService conversationService;
    private final NotificationService notificationService;

    public CommunicationController(
            AuthenticatedUserService authenticatedUserService,
            ConversationService conversationService,
            NotificationService notificationService
    ) {
        this.authenticatedUserService = authenticatedUserService;
        this.conversationService = conversationService;
        this.notificationService = notificationService;
    }

    @GetMapping("/conversations")
    @Operation(summary = "Listar mis conversaciones")
    public List<ConversationResponse> listConversations(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false)
            String authorization
    ) {
        AuthenticatedUserResponse user =
                authenticatedUserService.requireUser(authorization);
        return conversationService.listMine(user.idUsuario());
    }

    @GetMapping("/conversations/{conversationId}/messages")
    @Operation(summary = "Listar mensajes de una conversacion")
    public List<MessageResponse> listMessages(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false)
            String authorization,
            @PathVariable Integer conversationId
    ) {
        AuthenticatedUserResponse user =
                authenticatedUserService.requireUser(authorization);
        return conversationService.listMessages(
                user.idUsuario(),
                conversationId
        );
    }

    @PostMapping("/conversations/{conversationId}/messages")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Enviar un mensaje")
    public MessageResponse sendMessage(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false)
            String authorization,
            @PathVariable Integer conversationId,
            @Valid @RequestBody CreateMessageRequest request
    ) {
        AuthenticatedUserResponse user =
                authenticatedUserService.requireUser(authorization);
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
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false)
            String authorization,
            @PathVariable Integer conversationId
    ) {
        AuthenticatedUserResponse user =
                authenticatedUserService.requireUser(authorization);
        conversationService.markRead(user.idUsuario(), conversationId);
    }

    @GetMapping("/notifications")
    @Operation(summary = "Listar mis notificaciones")
    public List<NotificationResponse> listNotifications(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false)
            String authorization
    ) {
        AuthenticatedUserResponse user =
                authenticatedUserService.requireUser(authorization);
        return notificationService.listMine(user.idUsuario());
    }

    @PostMapping("/notifications/{notificationId}/read")
    @Operation(summary = "Marcar una notificacion como leida")
    public NotificationResponse markNotificationRead(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false)
            String authorization,
            @PathVariable Integer notificationId
    ) {
        AuthenticatedUserResponse user =
                authenticatedUserService.requireUser(authorization);
        return notificationService.markRead(
                user.idUsuario(),
                notificationId
        );
    }

    @PostMapping("/notifications/read-all")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Marcar todas las notificaciones como leidas")
    public void markAllNotificationsRead(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false)
            String authorization
    ) {
        AuthenticatedUserResponse user =
                authenticatedUserService.requireUser(authorization);
        notificationService.markAllRead(user.idUsuario());
    }
}
