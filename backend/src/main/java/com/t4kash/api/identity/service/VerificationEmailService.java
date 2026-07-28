package com.t4kash.api.identity.service;

import com.t4kash.api.exception.EmailDeliveryException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class VerificationEmailService {
    private final JavaMailSender mailSender;
    private final BrevoEmailClient brevoEmailClient;
    private final boolean enabled;
    private final String from;
    private final String provider;

    public VerificationEmailService(
            JavaMailSender mailSender,
            BrevoEmailClient brevoEmailClient,
            @Value("${app.mail.enabled:false}") boolean enabled,
            @Value("${app.mail.from:}") String from,
            @Value("${app.mail.provider:smtp}") String provider
    ) {
        this.mailSender = mailSender;
        this.brevoEmailClient = brevoEmailClient;
        this.enabled = enabled;
        this.from = from;
        this.provider = provider == null ? "" : provider.trim().toLowerCase();
    }

    public void sendCode(String recipient, String code, int expirationMinutes) {
        if (!enabled || from == null || from.isBlank()) {
            throw new EmailDeliveryException(
                    "El envio de correos de verificacion no esta configurado."
            );
        }

        if ("brevo".equals(provider)) {
            brevoEmailClient.sendCode(recipient, code, expirationMinutes);
            return;
        }
        if (!"smtp".equals(provider)) {
            throw new EmailDeliveryException(
                    "El proveedor de correo configurado no es compatible."
            );
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(recipient);
        message.setSubject("Codigo de verificacion de T4KASH");
        message.setText("""
                Tu codigo de verificacion es:

                %s

                El codigo vence en %d minutos. Si no solicitaste esta cuenta, ignora este mensaje.
                """.formatted(code, expirationMinutes));

        try {
            mailSender.send(message);
        } catch (MailException ex) {
            throw new EmailDeliveryException(
                    "No se pudo enviar el codigo de verificacion.",
                    ex
            );
        }
    }
}
