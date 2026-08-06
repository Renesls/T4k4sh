package com.t4kash.api.identity.service;

import com.t4kash.api.exception.InvalidCredentialsException;
import com.t4kash.api.identity.dto.MessageResponse;
import com.t4kash.api.identity.dto.ResetPasswordRequest;
import com.t4kash.api.identity.entity.TokenRecuperacionPassword;
import com.t4kash.api.identity.repository.TokenRecuperacionPasswordRepository;
import com.t4kash.api.marketplace.entity.Usuario;
import com.t4kash.api.marketplace.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

/** Owns the forgot-password / reset-password flow. */
@Service
public class PasswordResetService {
    private static final String ACTIVE_USER = "ACTIVO";

    private final UsuarioRepository usuarioRepository;
    private final TokenRecuperacionPasswordRepository recoveryTokenRepository;
    private final SessionTokenService tokenService;
    private final VerificationCodeService codeService;
    private final VerificationEmailService emailService;
    private final LoginSecurityService loginSecurityService;
    private final AuthSessionService authSessionService;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(12);
    private final int passwordResetMinutes;
    private final int resendSeconds;

    public PasswordResetService(
            UsuarioRepository usuarioRepository,
            TokenRecuperacionPasswordRepository recoveryTokenRepository,
            SessionTokenService tokenService,
            VerificationCodeService codeService,
            VerificationEmailService emailService,
            LoginSecurityService loginSecurityService,
            AuthSessionService authSessionService,
            @Value("${app.auth.password-reset-minutes:15}") int passwordResetMinutes,
            @Value("${app.auth.verification-resend-seconds:60}") int resendSeconds
    ) {
        this.usuarioRepository = usuarioRepository;
        this.recoveryTokenRepository = recoveryTokenRepository;
        this.tokenService = tokenService;
        this.codeService = codeService;
        this.emailService = emailService;
        this.loginSecurityService = loginSecurityService;
        this.authSessionService = authSessionService;
        this.passwordResetMinutes = passwordResetMinutes;
        this.resendSeconds = resendSeconds;
    }

    @Transactional
    public MessageResponse requestPasswordReset(String requestedEmail) {
        String correo = normalizeEmail(requestedEmail);
        MessageResponse genericResponse = new MessageResponse(
                "Si la cuenta existe, enviaremos un codigo de recuperacion."
        );
        Usuario usuario = usuarioRepository.findByCorreoIgnoreCase(correo)
                .filter(item -> ACTIVE_USER.equalsIgnoreCase(item.getEstadoUsuario()))
                .orElse(null);
        if (usuario == null) {
            return genericResponse;
        }

        LocalDateTime now = now();
        TokenRecuperacionPassword latest = recoveryTokenRepository
                .findFirstByIdUsuarioAndUsadoFalseOrderByFechaCreacionDesc(
                        usuario.getIdUsuario()
                )
                .orElse(null);
        if (latest != null
                && latest.getFechaCreacion().plusSeconds(resendSeconds).isAfter(now)) {
            return genericResponse;
        }
        recoveryTokenRepository.findAllByIdUsuarioAndUsadoFalse(usuario.getIdUsuario())
                .forEach(previous -> {
                    previous.setUsado(true);
                    previous.setFechaUso(now);
                    recoveryTokenRepository.save(previous);
                });

        String code = codeService.generate();
        TokenRecuperacionPassword recovery = new TokenRecuperacionPassword();
        recovery.setIdUsuario(usuario.getIdUsuario());
        recovery.setTokenHash(tokenService.hash(code));
        recovery.setFechaCreacion(now);
        recovery.setFechaExpiracion(now.plusMinutes(passwordResetMinutes));
        recovery.setUsado(false);
        recoveryTokenRepository.save(recovery);
        emailService.sendPasswordResetCode(correo, code, passwordResetMinutes);
        return genericResponse;
    }

    @Transactional
    public MessageResponse resetPassword(
            ResetPasswordRequest request,
            String ipOrigen,
            String userAgent
    ) {
        String correo = normalizeEmail(request.correo());
        loginSecurityService.requireAvailable(correo);
        Usuario usuario = usuarioRepository.findByCorreoIgnoreCase(correo)
                .orElseThrow(() -> new InvalidCredentialsException(
                        "El codigo no es valido o ha expirado."
                ));
        TokenRecuperacionPassword recovery = recoveryTokenRepository
                .findFirstByIdUsuarioAndUsadoFalseOrderByFechaCreacionDesc(
                        usuario.getIdUsuario()
                )
                .orElseThrow(() -> new InvalidCredentialsException(
                        "El codigo no es valido o ha expirado."
                ));

        if (!recovery.getFechaExpiracion().isAfter(now())
                || !tokenService.matches(request.codigo(), recovery.getTokenHash())) {
            loginSecurityService.recordFailure(
                    usuario.getIdUsuario(),
                    correo,
                    "CODIGO_RECUPERACION_INVALIDO",
                    ipOrigen,
                    userAgent
            );
            throw new InvalidCredentialsException(
                    "El codigo no es valido o ha expirado."
            );
        }

        LocalDateTime now = now();
        usuario.setPasswordHash(passwordEncoder.encode(request.nuevaPassword()));
        usuarioRepository.save(usuario);
        recovery.setUsado(true);
        recovery.setFechaUso(now);
        recoveryTokenRepository.save(recovery);
        authSessionService.closeActiveSessions(usuario.getIdUsuario(), now);
        loginSecurityService.recordSuccess(
                usuario.getIdUsuario(),
                correo,
                ipOrigen,
                userAgent
        );
        return new MessageResponse(
                "Contrasena actualizada. Ya puedes iniciar sesion."
        );
    }

    private String normalizeEmail(String correo) {
        return correo == null ? "" : correo.trim().toLowerCase(Locale.ROOT);
    }

    private LocalDateTime now() {
        return LocalDateTime.now().truncatedTo(ChronoUnit.MICROS);
    }
}
