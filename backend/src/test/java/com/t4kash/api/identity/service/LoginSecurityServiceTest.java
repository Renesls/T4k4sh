package com.t4kash.api.identity.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.t4kash.api.exception.TooManyAttemptsException;
import com.t4kash.api.identity.entity.IntentoLogin;
import com.t4kash.api.identity.repository.IntentoLoginRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class LoginSecurityServiceTest {
    @Mock
    private IntentoLoginRepository intentoRepository;

    @Test
    void allowsLoginWhenFailureLimitWasNotReached() {
        LoginSecurityService service = service();
        when(intentoRepository
                .findFirstByCorreoIgnoreCaseAndExitosoTrueOrderByFechaIntentoDesc(
                        "student@example.com"
                ))
                .thenReturn(Optional.empty());
        when(intentoRepository
                .countByCorreoIgnoreCaseAndExitosoFalseAndFechaIntentoAfter(
                        eq("student@example.com"),
                        any(LocalDateTime.class)
                ))
                .thenReturn(4L);

        assertDoesNotThrow(() -> service.requireAvailable("student@example.com"));
    }

    @Test
    void blocksLoginAfterFiveRecentFailures() {
        LoginSecurityService service = service();
        IntentoLogin lastFailure = new IntentoLogin();
        lastFailure.setFechaIntento(LocalDateTime.now().minusMinutes(1));
        when(intentoRepository
                .findFirstByCorreoIgnoreCaseAndExitosoTrueOrderByFechaIntentoDesc(
                        "student@example.com"
                ))
                .thenReturn(Optional.empty());
        when(intentoRepository
                .countByCorreoIgnoreCaseAndExitosoFalseAndFechaIntentoAfter(
                        eq("student@example.com"),
                        any(LocalDateTime.class)
                ))
                .thenReturn(5L);
        when(intentoRepository
                .findFirstByCorreoIgnoreCaseAndExitosoFalseOrderByFechaIntentoDesc(
                        "student@example.com"
                ))
                .thenReturn(Optional.of(lastFailure));

        assertThrows(
                TooManyAttemptsException.class,
                () -> service.requireAvailable("student@example.com")
        );
    }

    @Test
    void recordsFailureMetadata() {
        LoginSecurityService service = service();

        service.recordFailure(
                7,
                "student@example.com",
                "CREDENCIALES_INVALIDAS",
                "127.0.0.1",
                "test-agent"
        );

        ArgumentCaptor<IntentoLogin> captor = ArgumentCaptor.forClass(
                IntentoLogin.class
        );
        verify(intentoRepository).save(captor.capture());
        IntentoLogin attempt = captor.getValue();
        org.junit.jupiter.api.Assertions.assertEquals(7, attempt.getIdUsuario());
        org.junit.jupiter.api.Assertions.assertEquals(
                "student@example.com",
                attempt.getCorreo()
        );
        org.junit.jupiter.api.Assertions.assertEquals(
                "CREDENCIALES_INVALIDAS",
                attempt.getMotivoFallo()
        );
        org.junit.jupiter.api.Assertions.assertFalse(attempt.isExitoso());
    }

    private LoginSecurityService service() {
        return new LoginSecurityService(intentoRepository, 5, 15, 15);
    }
}
