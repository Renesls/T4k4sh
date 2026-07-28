package com.t4kash.api.identity.service;

import com.t4kash.api.exception.AccountNotVerifiedException;
import com.t4kash.api.exception.InvalidCredentialsException;
import com.t4kash.api.exception.ResourceConflictException;
import com.t4kash.api.exception.ResourceNotFoundException;
import com.t4kash.api.identity.dto.AuthResponse;
import com.t4kash.api.identity.dto.AuthenticatedUserResponse;
import com.t4kash.api.identity.dto.LoginRequest;
import com.t4kash.api.identity.dto.LoginChallengeResponse;
import com.t4kash.api.identity.dto.MessageResponse;
import com.t4kash.api.identity.dto.RegisterRequest;
import com.t4kash.api.identity.dto.RegistrationResponse;
import com.t4kash.api.identity.dto.ResetPasswordRequest;
import com.t4kash.api.identity.dto.VerifyEmailRequest;
import com.t4kash.api.identity.dto.VerifyLoginRequest;
import com.t4kash.api.identity.entity.SesionUsuario;
import com.t4kash.api.identity.entity.TokenRecuperacionPassword;
import com.t4kash.api.identity.entity.Universidad;
import com.t4kash.api.identity.entity.VerificacionUsuario;
import com.t4kash.api.identity.repository.CarreraRepository;
import com.t4kash.api.identity.repository.SesionUsuarioRepository;
import com.t4kash.api.identity.repository.TokenRecuperacionPasswordRepository;
import com.t4kash.api.identity.repository.UniversidadRepository;
import com.t4kash.api.identity.repository.UsuarioRolRepository;
import com.t4kash.api.identity.repository.VerificacionUsuarioRepository;
import com.t4kash.api.marketplace.entity.Usuario;
import com.t4kash.api.marketplace.entity.UsuarioEstudiante;
import com.t4kash.api.marketplace.repository.UsuarioEstudianteRepository;
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
public class AuthService {
    private static final String ACTIVE_USER = "ACTIVO";
    private static final String PENDING_USER = "PENDIENTE_VERIFICACION";
    private static final String ACTIVE_SESSION = "ACTIVA";
    private static final String PENDING_VERIFICATION = "PENDIENTE";
    private static final String REPLACED_VERIFICATION = "REEMPLAZADA";
    private static final String VERIFIED = "VERIFICADO";
    private static final String REGISTRATION_VERIFICATION = "CORREO_INSTITUCIONAL";
    private static final String LOGIN_VERIFICATION = "INICIO_SESION_2FA";
    private static final int SESSION_DAYS = 7;

    private final UsuarioRepository usuarioRepository;
    private final UsuarioEstudianteRepository estudianteRepository;
    private final UsuarioRolRepository usuarioRolRepository;
    private final SesionUsuarioRepository sesionRepository;
    private final UniversidadRepository universidadRepository;
    private final CarreraRepository carreraRepository;
    private final VerificacionUsuarioRepository verificacionRepository;
    private final TokenRecuperacionPasswordRepository recoveryTokenRepository;
    private final SessionTokenService tokenService;
    private final VerificationCodeService codeService;
    private final VerificationEmailService emailService;
    private final LoginSecurityService loginSecurityService;
    private final RegistrationPolicyService registrationPolicyService;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(12);
    private final int verificationMinutes;
    private final int resendSeconds;
    private final int twoFactorMinutes;
    private final int passwordResetMinutes;

    public AuthService(
            UsuarioRepository usuarioRepository,
            UsuarioEstudianteRepository estudianteRepository,
            UsuarioRolRepository usuarioRolRepository,
            SesionUsuarioRepository sesionRepository,
            UniversidadRepository universidadRepository,
            CarreraRepository carreraRepository,
            VerificacionUsuarioRepository verificacionRepository,
            TokenRecuperacionPasswordRepository recoveryTokenRepository,
            SessionTokenService tokenService,
            VerificationCodeService codeService,
            VerificationEmailService emailService,
            LoginSecurityService loginSecurityService,
            RegistrationPolicyService registrationPolicyService,
            @Value("${app.auth.verification-minutes:15}") int verificationMinutes,
            @Value("${app.auth.verification-resend-seconds:60}") int resendSeconds,
            @Value("${app.auth.two-factor-minutes:10}") int twoFactorMinutes,
            @Value("${app.auth.password-reset-minutes:15}") int passwordResetMinutes
    ) {
        this.usuarioRepository = usuarioRepository;
        this.estudianteRepository = estudianteRepository;
        this.usuarioRolRepository = usuarioRolRepository;
        this.sesionRepository = sesionRepository;
        this.universidadRepository = universidadRepository;
        this.carreraRepository = carreraRepository;
        this.verificacionRepository = verificacionRepository;
        this.recoveryTokenRepository = recoveryTokenRepository;
        this.tokenService = tokenService;
        this.codeService = codeService;
        this.emailService = emailService;
        this.loginSecurityService = loginSecurityService;
        this.registrationPolicyService = registrationPolicyService;
        this.verificationMinutes = verificationMinutes;
        this.resendSeconds = resendSeconds;
        this.twoFactorMinutes = twoFactorMinutes;
        this.passwordResetMinutes = passwordResetMinutes;
    }

    @Transactional
    public RegistrationResponse register(RegisterRequest request) {
        String correo = normalizeEmail(request.correo());
        if (usuarioRepository.existsByCorreoIgnoreCase(correo)) {
            throw new ResourceConflictException("Ya existe una cuenta con ese correo.");
        }

        RegistrationProfile profile = registrationPolicyService.resolve(
                correo,
                request.idUniversidad(),
                request.idCarrera()
        );

        LocalDateTime now = now();
        Usuario usuario = new Usuario();
        usuario.setNombre(request.nombre().trim());
        usuario.setApellido(request.apellido().trim());
        usuario.setCorreo(correo);
        usuario.setPasswordHash(passwordEncoder.encode(request.password()));
        usuario.setFechaRegistro(now);
        usuario.setEstadoUsuario(PENDING_USER);
        usuario.setIdUniversidad(
                profile.university() == null
                        ? null
                        : profile.university().getIdUniversidad()
        );
        usuario = usuarioRepository.save(usuario);

        int assignedClientRole = usuarioRolRepository.assignRole(
                usuario.getIdUsuario(),
                "CLIENTE"
        );
        if (assignedClientRole == 0) {
            throw new IllegalStateException("No se encontro el rol CLIENTE.");
        }

        if (profile.student()) {
            int assignedStudentRole = usuarioRolRepository.assignRole(
                    usuario.getIdUsuario(),
                    "ESTUDIANTE"
            );
            if (assignedStudentRole == 0) {
                throw new IllegalStateException("No se encontro el rol ESTUDIANTE.");
            }
            UsuarioEstudiante estudiante = new UsuarioEstudiante();
            estudiante.setIdUsuario(usuario.getIdUsuario());
            estudiante.setIdCarrera(profile.careerId());
            estudiante.setEstadoPerfilEstudiante(PENDING_VERIFICATION);
            estudiante.setFechaCreacion(now);
            estudianteRepository.save(estudiante);
        }

        VerificacionUsuario verification = createVerification(
                usuario,
                now,
                REGISTRATION_VERIFICATION,
                verificationMinutes
        );
        emailService.sendCode(correo, verification.getCodigoVerificacion(), verificationMinutes);
        return registrationResponse(verification);
    }

    @Transactional
    public AuthResponse verifyEmail(
            VerifyEmailRequest request,
            String ipOrigen,
            String userAgent
    ) {
        String correo = normalizeEmail(request.correo());
        VerificacionUsuario verification = verificacionRepository
                .findFirstByCorreoInstitucionalIgnoreCaseAndTipoVerificacionOrderByFechaSolicitudDesc(
                        correo,
                        REGISTRATION_VERIFICATION
                )
                .filter(item -> PENDING_VERIFICATION.equals(item.getEstadoVerificacion()))
                .orElseThrow(() -> new IllegalArgumentException(
                        "No existe una verificacion pendiente para ese correo."
                ));

        if (verification.getFechaExpiracion() == null
                || !verification.getFechaExpiracion().isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("El codigo ha expirado. Solicita uno nuevo.");
        }
        if (!verification.getCodigoVerificacion().equals(request.codigo())) {
            throw new IllegalArgumentException("El codigo de verificacion es incorrecto.");
        }

        LocalDateTime now = now();
        Usuario usuario = usuarioRepository.findById(verification.getIdUsuario())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "La cuenta asociada ya no existe."
                ));
        usuario.setEstadoUsuario(ACTIVE_USER);
        usuarioRepository.save(usuario);

        estudianteRepository.findById(usuario.getIdUsuario())
                .ifPresent(estudiante -> {
                    estudiante.setEstadoPerfilEstudiante(ACTIVE_USER);
                    estudianteRepository.save(estudiante);
                });

        verification.setEstadoVerificacion(VERIFIED);
        verification.setFechaVerificacion(now);
        verification.setUltimaRevalidacion(now);
        verificacionRepository.save(verification);

        return createSession(usuario, ipOrigen, userAgent);
    }

    @Transactional
    public RegistrationResponse resendVerification(String requestedEmail) {
        String correo = normalizeEmail(requestedEmail);
        Usuario usuario = usuarioRepository.findByCorreoIgnoreCase(correo)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No existe una cuenta pendiente con ese correo."
                ));
        if (!PENDING_USER.equalsIgnoreCase(usuario.getEstadoUsuario())) {
            throw new ResourceConflictException("La cuenta ya se encuentra activa.");
        }

        LocalDateTime now = now();
        verificacionRepository
                .findFirstByCorreoInstitucionalIgnoreCaseAndTipoVerificacionOrderByFechaSolicitudDesc(
                        correo,
                        REGISTRATION_VERIFICATION
                )
                .ifPresent(previous -> {
                    if (previous.getFechaSolicitud().plusSeconds(resendSeconds).isAfter(now)) {
                        throw new ResourceConflictException(
                                "Espera un momento antes de solicitar otro codigo."
                        );
                    }
                    if (PENDING_VERIFICATION.equals(previous.getEstadoVerificacion())) {
                        previous.setEstadoVerificacion(REPLACED_VERIFICATION);
                        verificacionRepository.save(previous);
                    }
                });

        VerificacionUsuario verification = createVerification(
                usuario,
                now,
                REGISTRATION_VERIFICATION,
                verificationMinutes
        );
        emailService.sendCode(correo, verification.getCodigoVerificacion(), verificationMinutes);
        return registrationResponse(verification);
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
        VerificacionUsuario verification = createVerification(
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
        AuthResponse response = createSession(usuario, ipOrigen, userAgent);
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

        VerificacionUsuario verification = createVerification(
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
        closeActiveSessions(usuario.getIdUsuario(), now);
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

    @Transactional
    public AuthenticatedUserResponse getCurrentUser(String rawToken) {
        SesionUsuario sesion = requireActiveSession(rawToken);
        Usuario usuario = usuarioRepository.findById(sesion.getIdUsuario())
                .orElseThrow(() -> new InvalidCredentialsException(
                        "La cuenta de la sesion ya no existe."
                ));
        if (!ACTIVE_USER.equalsIgnoreCase(usuario.getEstadoUsuario())) {
            throw new InvalidCredentialsException("La cuenta no se encuentra activa.");
        }
        return toUserResponse(usuario);
    }

    @Transactional
    public void logout(String rawToken) {
        SesionUsuario sesion = requireActiveSession(rawToken);
        sesion.setEstadoSesion("CERRADA");
        sesion.setFechaCierre(now());
        sesionRepository.save(sesion);
    }

    private VerificacionUsuario createVerification(
            Usuario usuario,
            LocalDateTime now,
            String verificationType,
            int expirationMinutes
    ) {
        VerificacionUsuario verification = new VerificacionUsuario();
        verification.setIdUsuario(usuario.getIdUsuario());
        verification.setCorreoInstitucional(usuario.getCorreo());
        verification.setCodigoVerificacion(codeService.generate());
        verification.setEstadoVerificacion(PENDING_VERIFICATION);
        verification.setFechaSolicitud(now);
        verification.setFechaExpiracion(now.plusMinutes(expirationMinutes));
        verification.setTipoVerificacion(verificationType);
        return verificacionRepository.save(verification);
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

    private void closeActiveSessions(Integer userId, LocalDateTime now) {
        sesionRepository.findAllByIdUsuarioAndEstadoSesion(userId, ACTIVE_SESSION)
                .forEach(session -> {
                    session.setEstadoSesion("CERRADA");
                    session.setFechaCierre(now);
                    sesionRepository.save(session);
                });
    }

    private RegistrationResponse registrationResponse(
            VerificacionUsuario verification
    ) {
        return new RegistrationResponse(
                verification.getCorreoInstitucional(),
                verification.getFechaExpiracion(),
                "Enviamos un codigo de verificacion a tu correo."
        );
    }

    private AuthResponse createSession(
            Usuario usuario,
            String ipOrigen,
            String userAgent
    ) {
        String rawToken = tokenService.generateToken();
        LocalDateTime now = now();

        SesionUsuario sesion = new SesionUsuario();
        sesion.setIdUsuario(usuario.getIdUsuario());
        sesion.setTokenHash(tokenService.hash(rawToken));
        sesion.setIpOrigen(limit(ipOrigen, 45));
        sesion.setUserAgent(limit(userAgent, 500));
        sesion.setFechaInicio(now);
        sesion.setFechaExpiracion(now.plusDays(SESSION_DAYS));
        sesion.setEstadoSesion(ACTIVE_SESSION);
        sesionRepository.save(sesion);

        return new AuthResponse(
                rawToken,
                sesion.getFechaExpiracion(),
                toUserResponse(usuario)
        );
    }

    private SesionUsuario requireActiveSession(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new InvalidCredentialsException("Debes iniciar sesion.");
        }

        SesionUsuario sesion = sesionRepository
                .findByTokenHashAndEstadoSesion(tokenService.hash(rawToken), ACTIVE_SESSION)
                .orElseThrow(() -> new InvalidCredentialsException(
                        "La sesion no es valida."
                ));

        if (!sesion.getFechaExpiracion().isAfter(LocalDateTime.now())) {
            sesion.setEstadoSesion("EXPIRADA");
            sesion.setFechaCierre(now());
            sesionRepository.save(sesion);
            throw new InvalidCredentialsException("La sesion ha expirado.");
        }
        return sesion;
    }

    private AuthenticatedUserResponse toUserResponse(Usuario usuario) {
        String universityName = usuario.getIdUniversidad() == null
                ? null
                : universidadRepository.findById(usuario.getIdUniversidad())
                .map(Universidad::getNombreUniversidad)
                .orElse(null);
        UsuarioEstudiante student = estudianteRepository
                .findById(usuario.getIdUsuario())
                .orElse(null);
        Integer careerId = student == null ? null : student.getIdCarrera();
        String careerName = careerId == null
                ? null
                : carreraRepository.findById(careerId)
                .map(career -> career.getNombreCarrera())
                .orElse(null);
        return new AuthenticatedUserResponse(
                usuario.getIdUsuario(),
                usuario.getNombre(),
                usuario.getApellido(),
                usuario.getCorreo(),
                usuario.getIdUniversidad(),
                universityName,
                careerId,
                careerName,
                usuario.getEstadoUsuario(),
                usuarioRolRepository.findRoleNames(usuario.getIdUsuario())
        );
    }

    private String normalizeEmail(String correo) {
        return correo == null ? "" : correo.trim().toLowerCase(Locale.ROOT);
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
