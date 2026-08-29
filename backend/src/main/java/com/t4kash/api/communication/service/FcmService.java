package com.t4kash.api.communication.service;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class FcmService {
    private static final Logger LOGGER = LoggerFactory.getLogger(FcmService.class);

    public void sendPushNotification(String targetToken, String title, String body) {
        Notification notification = Notification.builder()
                .setTitle(title)
                .setBody(body)
                .build();

        Message message = Message.builder()
                .setNotification(notification)
                .setToken(targetToken)
                .build();

        try {
            String response = FirebaseMessaging.getInstance().send(message);
            LOGGER.info("Notificacion push enviada a Firebase. ID: {}", response);
        } catch (Exception e) {
            LOGGER.warn("No se pudo enviar la notificacion push: {}", e.getMessage());
        }
    }
}