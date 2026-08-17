package com.t4kash.api.identity.service;

import com.t4kash.api.exception.AccountNotVerifiedException;
import com.t4kash.api.exception.InvalidCredentialsException;
import com.t4kash.api.exception.ResourceConflictException;
import com.t4kash.api.identity.dto.AuthResponse;
import com.t4kash.api.identity.dto.LoginChallengeResponse;
import com.t4kash.api.identity.dto.LoginRequest;
import com.t4kash.api.identity.dto.VerifyLoginRequest;
import com.t4kash.api.identity.entity.VerificacionUsuario;
import com.t4kash.api.identity.repository.VerificacionUsuarioRepository;
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

@Service
public class LoginService {
    private static final String ACTIVE_USER = "ACTIVO";
    private static final String PENDING_USER = "PENDIENTE_VERIFICACION";
    private static final String PENDING_VERIFICATION = "PENDIENTE";
    private static final String REPLACED_VERIFICATION = "REEMPLAZADA";
    private static final String VERIFIED = "VERIFICADO";
    private static final String LOGIN_VERIFICATION = "INICIO_SESION_2FA";

    private final UsuarioRepository usuarioRepository;
    private final VerificacionUsuarioRepository verificacionRepository;
    private final VerificationRecordService verificationRecordService;
    private final VerificationEmailService emailService;
    private final LoginSecurityService loginSecurityService;
    private final AuthSessionService authSessionService;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(12);
    private final int twoFactorMinutes;
    private final int resendSeconds;

    public LoginService(
            UsuarioRepository usuarioRepository,
            VerificacionUsuarioRepository verificacionRepository,
            VerificationRecordService verificationRecordService,
            VerificationEmailService emailService,
            LoginSecurityService loginSecurityService,
            AuthSessionService authSessionService,
            @Value("${app.auth.two-factor-minutes:10}") int twoFactorMinutes,
            @Value("${app.auth.verification-resend-seconds:60}") int resendSeconds
    ) {
        this.usuarioRepository = usuarioRepository;
        this.verificacionRepository = verificacionRepository;
        this.verificationRecordService = verificationRecordService;
        this.emailService = emailService;
        this.loginSecurityService = loginSecurityService;
        this.authSessionService = authSessionService;
        this.twoFactorMinutes = twoFactorMinutes;
        this.resendSeconds = resendSeconds;
    }

    @Transactional
    public LoginChallengeResponse login(
            LoginRequest request,
            String ipOrigen,
            String userAgent
    ) {
        String correo = normalizeEmail(request.correo());
        loginSecurityService.requireAvailable(correo);
        Usuario usuario = usuarioRepository.findByCorreoIgnoreCase(correo).orElse(null);

        if (usuario == null
                || !passwordEncoder.matches(request.password(), usuario.getPasswordHash())) {
            loginSecurityService.recordFailure(
                    usuario == null ? null : usuario.getIdUsuario(),
                    correo,
                    "CREDENCIALES_INVALIDAS",
                    ipOrigen,
                    userAgent
            );
            throw new InvalidCredentialsException("Correo o contrasena incorrectos.");
        }
        if (PENDING_USER.equalsIgnoreCase(usuario.getEstadoUsuario())) {
            loginSecurityService.recordFailure(
                    usuario.getIdUsuario(),
                    correo,
                    "CUENTA_NO_VERIFICADA",
                    ipOrigen,
                    userAgent
            );
            throw new AccountNotVerifiedException(
                    "Debes verificar tu correo antes de iniciar sesion."
            );
        }
        if (!ACTIVE_USER.equalsIgnoreCase(usuario.getEstadoUsuario())) {
            loginSecurityService.recordFailure(
                    usuario.getIdUsuario(),
                    correo,
                    "CUENTA_INACTIVA",
                    ipOrigen,
                    userAgent
            );
            throw new InvalidCredentialsException("La cuenta no se encuentra activa.");
        }

        // ---> MODIFICACIÓN FCM TOKEN: Guardamos el token en la BD <---
        if (request.fcmToken() != null && !request.fcmToken().isEmpty()) {
            usuario.setFcmToken(request.fcmToken());
            usuarioRepository.save(usuario);
        }
        // -------------------------------------------------------------

        LocalDateTime now = now();
        VerificacionUsuario previous = verificacionRepository
                .findFirstByCorreoInstitucionalIgnoreCaseAndTipoVerificacionOrderByFechaSolicitudDesc(
                        correo,
                        LOGIN_VERIFICATION
                )
                .orElse(null);
        if (previous != null
                && PENDING_VERIFICATION.equals(previous.getEstadoVerificacion())
                && previous.getFechaSolicitud().plusSeconds(resendSeconds).isAfter(now)) {
            return loginChallengeResponse(previous);
        }
        if (previous != null
                && PENDING_VERIFICATION.equals(previous.getEstadoVerificacion())) {
            previous.setEstadoVerificacion(REPLACED_VERIFICATION);
            verificacionRepository.save(previous);
        }
        VerificacionUsuario verification = verificationRecordService.create(
                usuario,
                now,
                LOGIN_VERIFICATION,
                twoFactorMinutes
        );
        emailService.sendLoginCode(
                correo,
                verification.getCodigoVerificacion(),
                twoFactorMinutes
        );
        return loginChallengeResponse(verification);
    }

    @Transactional
    public AuthResponse verifyLogin(
            VerifyLoginRequest request,
            String ipOrigen,
            String userAgent
    ) {
        String correo = normalizeEmail(request.correo());
        loginSecurityService.requireAvailable(correo);
        Usuario usuario = usuarioRepository.findByCorreoIgnoreCase(correo)
                .orElseThrow(() -> new InvalidCredentialsException(
                        "El codigo no es valido o ha expirado."
                ));
        VerificacionUsuario verification = verificacionRepository
                .findFirstByCorreoInstitucionalIgnoreCaseAndTipoVerificacionOrderByFechaSolicitudDesc(
                        correo,
                        LOGIN_VERIFICATION
                )
                .filter(item -> PENDING_VERIFICATION.equals(item.getEstadoVerificacion()))
                .orElseThrow(() -> new InvalidCredentialsException(
                        "El codigo no es valido o ha expirado."
                ));

        if (verification.getFechaExpiracion() == null
                || !verification.getFechaExpiracion().isAfter(now())) {
            loginSecurityService.recordFailure(
                    usuario.getIdUsuario(),
                    correo,
                    "CODIGO_2FA_EXPIRADO",
                    ipOrigen,
                    userAgent
            );
            throw new InvalidCredentialsException("El codigo no es valido o ha expirado.");
        }
        if (!verification.getCodigoVerificacion().equals(request.codigo())) {
            loginSecurityService.recordFailure(
                    usuario.getIdUsuario(),
                    correo,
                    "CODIGO_2FA_INVALIDO",
                    ipOrigen,
                    userAgent
            );
            throw new InvalidCredentialsException("El codigo no es valido o ha expirado.");
        }

        LocalDateTime now = now();
        verification.setEstadoVerificacion(VERIFIED);
        verification.setFechaVerificacion(now);
        verification.setUltimaRevalidacion(now);
        verificacionRepository.save(verification);
        AuthResponse response = authSessionService.createSession(usuario, ipOrigen, userAgent);
        loginSecurityService.recordSuccess(
                usuario.getIdUsuario(),
                correo,
                ipOrigen,
                userAgent
        );
        return response;
    }

    @Transactional
    public LoginChallengeResponse resendLoginVerification(String requestedEmail) {
        String correo = normalizeEmail(requestedEmail);
        Usuario usuario = usuarioRepository.findByCorreoIgnoreCase(correo)
                .filter(item -> ACTIVE_USER.equalsIgnoreCase(item.getEstadoUsuario()))
                .orElseThrow(() -> new InvalidCredentialsException(
                        "No se puede reenviar el codigo."
                ));
        LocalDateTime now = now();
        VerificacionUsuario previous = verificacionRepository
                .findFirstByCorreoInstitucionalIgnoreCaseAndTipoVerificacionOrderByFechaSolicitudDesc(
                        correo,
                        LOGIN_VERIFICATION
                )
                .orElseThrow(() -> new InvalidCredentialsException(
                        "Primero ingresa tu correo y contrasena."
                ));
        if (previous.getFechaSolicitud().plusSeconds(resendSeconds).isAfter(now)) {
            throw new ResourceConflictException(
                    "Espera un momento antes de solicitar otro codigo."
            );
        }
        if (PENDING_VERIFICATION.equals(previous.getEstadoVerificacion())) {
            previous.setEstadoVerificacion(REPLACED_VERIFICATION);
            verificacionRepository.save(previous);
        }

        VerificacionUsuario verification = verificationRecordService.create(
                usuario,
                now,
                LOGIN_VERIFICATION,
                twoFactorMinutes
        );
        emailService.sendLoginCode(
                correo,
                verification.getCodigoVerificacion(),
                twoFactorMinutes
        );
        return loginChallengeResponse(verification);
    }

    private LoginChallengeResponse loginChallengeResponse(
            VerificacionUsuario verification
    ) {
        return new LoginChallengeResponse(
                verification.getCorreoInstitucional(),
                verification.getFechaExpiracion(),
                "Enviamos un codigo para confirmar tu inicio de sesion."
        );
    }

    private String normalizeEmail(String correo) {
        return correo == null ? "" : correo.trim().toLowerCase(Locale.ROOT);
    }

    private LocalDateTime now() {
        return LocalDateTime.now().truncatedTo(ChronoUnit.MICROS);
    }
}