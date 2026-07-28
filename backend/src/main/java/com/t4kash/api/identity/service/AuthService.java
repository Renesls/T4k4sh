package com.t4kash.api.identity.service;

import com.t4kash.api.exception.AccountNotVerifiedException;
import com.t4kash.api.exception.InvalidCredentialsException;
import com.t4kash.api.exception.ResourceConflictException;
import com.t4kash.api.exception.ResourceNotFoundException;
import com.t4kash.api.identity.dto.AuthResponse;
import com.t4kash.api.identity.dto.AuthenticatedUserResponse;
import com.t4kash.api.identity.dto.LoginRequest;
import com.t4kash.api.identity.dto.RegisterRequest;
import com.t4kash.api.identity.dto.RegistrationResponse;
import com.t4kash.api.identity.dto.VerifyEmailRequest;
import com.t4kash.api.identity.entity.SesionUsuario;
import com.t4kash.api.identity.entity.Universidad;
import com.t4kash.api.identity.entity.VerificacionUsuario;
import com.t4kash.api.identity.repository.CarreraRepository;
import com.t4kash.api.identity.repository.SesionUsuarioRepository;
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
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AuthService {
    private static final String ACTIVE_USER = "ACTIVO";
    private static final String PENDING_USER = "PENDIENTE_VERIFICACION";
    private static final String ACTIVE_SESSION = "ACTIVA";
    private static final String PENDING_VERIFICATION = "PENDIENTE";
    private static final int SESSION_DAYS = 7;

    private final UsuarioRepository usuarioRepository;
    private final UsuarioEstudianteRepository estudianteRepository;
    private final UsuarioRolRepository usuarioRolRepository;
    private final SesionUsuarioRepository sesionRepository;
    private final UniversidadRepository universidadRepository;
    private final CarreraRepository carreraRepository;
    private final VerificacionUsuarioRepository verificacionRepository;
    private final SessionTokenService tokenService;
    private final VerificationCodeService codeService;
    private final VerificationEmailService emailService;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(12);
    private final Set<String> evaluatorEmails;
    private final int verificationMinutes;
    private final int resendSeconds;

    public AuthService(
            UsuarioRepository usuarioRepository,
            UsuarioEstudianteRepository estudianteRepository,
            UsuarioRolRepository usuarioRolRepository,
            SesionUsuarioRepository sesionRepository,
            UniversidadRepository universidadRepository,
            CarreraRepository carreraRepository,
            VerificacionUsuarioRepository verificacionRepository,
            SessionTokenService tokenService,
            VerificationCodeService codeService,
            VerificationEmailService emailService,
            @Value("${app.auth.evaluator-emails:}") String evaluatorEmails,
            @Value("${app.auth.verification-minutes:15}") int verificationMinutes,
            @Value("${app.auth.verification-resend-seconds:60}") int resendSeconds
    ) {
        this.usuarioRepository = usuarioRepository;
        this.estudianteRepository = estudianteRepository;
        this.usuarioRolRepository = usuarioRolRepository;
        this.sesionRepository = sesionRepository;
        this.universidadRepository = universidadRepository;
        this.carreraRepository = carreraRepository;
        this.verificacionRepository = verificacionRepository;
        this.tokenService = tokenService;
        this.codeService = codeService;
        this.emailService = emailService;
        this.evaluatorEmails = parseEmails(evaluatorEmails);
        this.verificationMinutes = verificationMinutes;
        this.resendSeconds = resendSeconds;
    }

    @Transactional
    public RegistrationResponse register(RegisterRequest request) {
        String correo = normalizeEmail(request.correo());
        if (usuarioRepository.existsByCorreoIgnoreCase(correo)) {
            throw new ResourceConflictException("Ya existe una cuenta con ese correo.");
        }

        Universidad universidad = universidadRepository
                .findByIdUniversidadAndEstadoTrue(request.idUniversidad())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "La universidad seleccionada no existe o esta inactiva."
                ));
        carreraRepository.findByIdCarreraAndIdUniversidad(
                request.idCarrera(),
                universidad.getIdUniversidad()
        ).orElseThrow(() -> new IllegalArgumentException(
                "La carrera no pertenece a la universidad seleccionada."
        ));
        validateInstitutionalEmail(correo, universidad);

        LocalDateTime now = now();
        Usuario usuario = new Usuario();
        usuario.setNombre(request.nombre().trim());
        usuario.setApellido(request.apellido().trim());
        usuario.setCorreo(correo);
        usuario.setPasswordHash(passwordEncoder.encode(request.password()));
        usuario.setFechaRegistro(now);
        usuario.setEstadoUsuario(PENDING_USER);
        usuario.setIdUniversidad(universidad.getIdUniversidad());
        usuario = usuarioRepository.save(usuario);

        int assignedRoles = usuarioRolRepository.assignMarketplaceRoles(usuario.getIdUsuario());
        if (assignedRoles == 0) {
            throw new IllegalStateException("No se encontraron los roles base del marketplace.");
        }

        UsuarioEstudiante estudiante = new UsuarioEstudiante();
        estudiante.setIdUsuario(usuario.getIdUsuario());
        estudiante.setIdCarrera(request.idCarrera());
        estudiante.setEstadoPerfilEstudiante(PENDING_VERIFICATION);
        estudiante.setFechaCreacion(now);
        estudianteRepository.save(estudiante);

        VerificacionUsuario verification = createVerification(usuario, now);
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
                .findFirstByCorreoInstitucionalIgnoreCaseOrderByFechaSolicitudDesc(correo)
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

        UsuarioEstudiante estudiante = estudianteRepository.findById(usuario.getIdUsuario())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "El perfil estudiantil asociado ya no existe."
                ));
        estudiante.setEstadoPerfilEstudiante(ACTIVE_USER);
        estudianteRepository.save(estudiante);

        verification.setEstadoVerificacion("VERIFICADO");
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
                .findFirstByCorreoInstitucionalIgnoreCaseOrderByFechaSolicitudDesc(correo)
                .ifPresent(previous -> {
                    if (previous.getFechaSolicitud().plusSeconds(resendSeconds).isAfter(now)) {
                        throw new ResourceConflictException(
                                "Espera un momento antes de solicitar otro codigo."
                        );
                    }
                    if (PENDING_VERIFICATION.equals(previous.getEstadoVerificacion())) {
                        previous.setEstadoVerificacion("REEMPLAZADA");
                        verificacionRepository.save(previous);
                    }
                });

        VerificacionUsuario verification = createVerification(usuario, now);
        emailService.sendCode(correo, verification.getCodigoVerificacion(), verificationMinutes);
        return registrationResponse(verification);
    }

    @Transactional
    public AuthResponse login(
            LoginRequest request,
            String ipOrigen,
            String userAgent
    ) {
        Usuario usuario = usuarioRepository.findByCorreoIgnoreCase(normalizeEmail(request.correo()))
                .orElseThrow(() -> new InvalidCredentialsException(
                        "Correo o contrasena incorrectos."
                ));

        if (!passwordEncoder.matches(request.password(), usuario.getPasswordHash())) {
            throw new InvalidCredentialsException("Correo o contrasena incorrectos.");
        }
        if (PENDING_USER.equalsIgnoreCase(usuario.getEstadoUsuario())) {
            throw new AccountNotVerifiedException(
                    "Debes verificar tu correo antes de iniciar sesion."
            );
        }
        if (!ACTIVE_USER.equalsIgnoreCase(usuario.getEstadoUsuario())) {
            throw new InvalidCredentialsException("La cuenta no se encuentra activa.");
        }

        return createSession(usuario, ipOrigen, userAgent);
    }

    @Transactional
    public AuthenticatedUserResponse getCurrentUser(String rawToken) {
        SesionUsuario sesion = requireActiveSession(rawToken);
        Usuario usuario = usuarioRepository.findById(sesion.getIdUsuario())
                .orElseThrow(() -> new InvalidCredentialsException(
                        "La cuenta de la sesion ya no existe."
                ));
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
            LocalDateTime now
    ) {
        VerificacionUsuario verification = new VerificacionUsuario();
        verification.setIdUsuario(usuario.getIdUsuario());
        verification.setCorreoInstitucional(usuario.getCorreo());
        verification.setCodigoVerificacion(codeService.generate());
        verification.setEstadoVerificacion(PENDING_VERIFICATION);
        verification.setFechaSolicitud(now);
        verification.setFechaExpiracion(now.plusMinutes(verificationMinutes));
        verification.setTipoVerificacion("CORREO_INSTITUCIONAL");
        return verificacionRepository.save(verification);
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

    private void validateInstitutionalEmail(
            String correo,
            Universidad universidad
    ) {
        if (evaluatorEmails.contains(correo)) {
            return;
        }
        String emailDomain = correo.substring(correo.lastIndexOf('@') + 1);
        String universityDomain = universidad.getDominioCorreo()
                .trim()
                .toLowerCase(Locale.ROOT)
                .replaceFirst("^@", "");
        if (!emailDomain.equals(universityDomain)) {
            throw new IllegalArgumentException(
                    "El correo no pertenece al dominio de la universidad seleccionada."
            );
        }
    }

    private Set<String> parseEmails(String configuredEmails) {
        return Arrays.stream(configuredEmails.split(","))
                .map(this::normalizeEmail)
                .filter(value -> !value.isBlank())
                .collect(Collectors.toUnmodifiableSet());
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
