package com.t4kash.api.identity.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.t4kash.api.exception.EmailDeliveryException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;

@ExtendWith(MockitoExtension.class)
class VerificationEmailServiceTest {
    @Mock
    private JavaMailSender mailSender;

    @Mock
    private BrevoEmailClient brevoEmailClient;

    @Test
    void delegatesToBrevoWhenConfigured() {
        VerificationEmailService service = new VerificationEmailService(
                mailSender,
                brevoEmailClient,
                true,
                "sender@example.com",
                "brevo"
        );

        service.sendCode("student@example.com", "123456", 15);

        verify(brevoEmailClient).sendCode("student@example.com", "123456", 15);
        verifyNoInteractions(mailSender);
    }

    @Test
    void rejectsSendingWhenMailIsDisabled() {
        VerificationEmailService service = new VerificationEmailService(
                mailSender,
                brevoEmailClient,
                false,
                "sender@example.com",
                "brevo"
        );

        assertThrows(
                EmailDeliveryException.class,
                () -> service.sendCode("student@example.com", "123456", 15)
        );
        verifyNoInteractions(mailSender, brevoEmailClient);
    }

    @Test
    void rejectsUnknownProvider() {
        VerificationEmailService service = new VerificationEmailService(
                mailSender,
                brevoEmailClient,
                true,
                "sender@example.com",
                "unknown"
        );

        assertThrows(
                EmailDeliveryException.class,
                () -> service.sendCode("student@example.com", "123456", 15)
        );
        verifyNoInteractions(mailSender, brevoEmailClient);
    }
}
