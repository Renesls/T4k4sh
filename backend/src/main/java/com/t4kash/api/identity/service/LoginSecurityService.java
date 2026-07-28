package com.t4kash.api.identity.service;

import com.t4kash.api.exception.TooManyAttemptsException;
import com.t4kash.api.identity.entity.IntentoLogin;
import com.t4kash.api.identity.repository.IntentoLoginRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Service
public class LoginSecurityService {
    private final IntentoLoginRepository intentoRepository;
    private final int maxAttempts;
    private final int windowMinutes;
    private final int lockMinutes;

    public LoginSecurityService(
            IntentoLoginRepository intentoRepository,
            @Value("${app.auth.login-max-attempts:5}") int maxAttempts,
            @Value("${app.auth.login-window-minutes:15}") int windowMinutes,
            @Value("${app.auth.login-lock-minutes:15}") int lockMinutes
    ) {
        this.intentoRepository = intentoRepository;
        this.maxAttempts = maxAttempts;
        this.windowMinutes = windowMinutes;
        this.lockMinutes = lockMinutes;
    }

    public void requireAvailable(String correo) {
        LocalDateTime now = now();
        LocalDateTime windowStart = now.minusMinutes(windowMinutes);
        LocalDateTime cutoff = intentoRepository
                .findFirstByCorreoIgnoreCaseAndExitosoTrueOrderByFechaIntentoDesc(correo)
                .map(IntentoLogin::getFechaIntento)
                .filter(lastSuccess -> lastSuccess.isAfter(windowStart))
                .orElse(windowStart);

        long failures = intentoRepository
                .countByCorreoIgnoreCaseAndExitosoFalseAndFechaIntentoAfter(
                        correo,
                        cutoff
                );
        if (failures < maxAttempts) {
            return;
        }

        LocalDateTime lockedUntil = intentoRepository
                .findFirstByCorreoIgnoreCaseAndExitosoFalseOrderByFechaIntentoDesc(correo)
                .map(IntentoLogin::getFechaIntento)
                .map(lastFailure -> lastFailure.plusMinutes(lockMinutes))
                .orElse(now);
        if (!lockedUntil.isAfter(now)) {
            return;
        }

        long seconds = Math.max(1, Duration.between(now, lockedUntil).toSeconds());
        long minutes = Math.max(1, (seconds + 59) / 60);
        throw new TooManyAttemptsException(
                "Demasiados intentos fallidos. Intenta nuevamente en "
                        + minutes
                        + " minuto(s)."
        );
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailure(
            Integer userId,
            String correo,
            String reason,
            String ipOrigen,
            String userAgent
    ) {
        saveAttempt(userId, correo, false, reason, ipOrigen, userAgent);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordSuccess(
            Integer userId,
            String correo,
            String ipOrigen,
            String userAgent
    ) {
        saveAttempt(userId, correo, true, null, ipOrigen, userAgent);
    }

    private void saveAttempt(
            Integer userId,
            String correo,
            boolean successful,
            String reason,
            String ipOrigen,
            String userAgent
    ) {
        IntentoLogin attempt = new IntentoLogin();
        attempt.setIdUsuario(userId);
        attempt.setCorreo(limit(correo, 150));
        attempt.setExitoso(successful);
        attempt.setMotivoFallo(limit(reason, 150));
        attempt.setIpOrigen(limit(ipOrigen, 45));
        attempt.setUserAgent(limit(userAgent, 500));
        attempt.setFechaIntento(now());
        intentoRepository.save(attempt);
    }

    private LocalDateTime now() {
        return LocalDateTime.now().truncatedTo(ChronoUnit.MICROS);
    }

    private String limit(String value, int maxLength) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
