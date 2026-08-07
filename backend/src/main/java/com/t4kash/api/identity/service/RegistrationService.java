package com.t4kash.api.identity.service;

import com.t4kash.api.exception.ResourceConflictException;
import com.t4kash.api.exception.ResourceNotFoundException;
import com.t4kash.api.identity.dto.AuthResponse;
import com.t4kash.api.identity.dto.RegisterRequest;
import com.t4kash.api.identity.dto.RegistrationResponse;
import com.t4kash.api.identity.dto.VerifyEmailRequest;
import com.t4kash.api.identity.entity.VerificacionUsuario;
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

/** Gestiona la creacion de cuentas y la verificacion inicial por correo. */
@Service
public class RegistrationService {
    private static final String ACTIVE_USER = "ACTIVO";
    private static final String PENDING_USER = "PENDIENTE_VERIFICACION";
    private static final String PENDING_VERIFICATION = "PENDIENTE";
    private static final String REPLACED_VERIFICATION = "REEMPLAZADA";
    private static final String VERIFIED = "VERIFICADO";
    private static final String REGISTRATION_VERIFICATION = "CORREO_INSTITUCIONAL";

    private final UsuarioRepository usuarioRepository;
    private final UsuarioEstudianteRepository estudianteRepository;
    private final UsuarioRolRepository usuarioRolRepository;
    private final VerificacionUsuarioRepository verificacionRepository;
    private final VerificationRecordService verificationRecordService;
    private final VerificationEmailService emailService;
    private final RegistrationPolicyService registrationPolicyService;
    private final AuthSessionService authSessionService;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(12);
    private final int verificationMinutes;
    private final int resendSeconds;

    public RegistrationService(
            UsuarioRepository usuarioRepository,
            UsuarioEstudianteRepository estudianteRepository,
            UsuarioRolRepository usuarioRolRepository,
            VerificacionUsuarioRepository verificacionRepository,
            VerificationRecordService verificationRecordService,
            VerificationEmailService emailService,
            RegistrationPolicyService registrationPolicyService,
            AuthSessionService authSessionService,
            @Value("${app.auth.verification-minutes:15}") int verificationMinutes,
            @Value("${app.auth.verification-resend-seconds:60}") int resendSeconds
    ) {
        this.usuarioRepository = usuarioRepository;
        this.estudianteRepository = estudianteRepository;
        this.usuarioRolRepository = usuarioRolRepository;
        this.verificacionRepository = verificacionRepository;
        this.verificationRecordService = verificationRecordService;
        this.emailService = emailService;
        this.registrationPolicyService = registrationPolicyService;
        this.authSessionService = authSessionService;
        this.verificationMinutes = verificationMinutes;
        this.resendSeconds = resendSeconds;
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

        if (profile.studentRequested()) {
            if (!profile.automaticStudentAccess()
                    && (request.carnetUniversitario() == null
                    || request.carnetUniversitario().isBlank())) {
                throw new IllegalArgumentException(
                        "Ingresa tu carnet para solicitar la validacion estudiantil."
                );
            }
            if (profile.automaticStudentAccess()) {
                assignStudentRole(usuario.getIdUsuario());
            }
            UsuarioEstudiante estudiante = new UsuarioEstudiante();
            estudiante.setIdUsuario(usuario.getIdUsuario());
            estudiante.setIdCarrera(profile.careerId());
            estudiante.setCarnetUniversitario(
                    request.carnetUniversitario() == null
                            ? null
                            : request.carnetUniversitario().trim()
            );
            estudiante.setEstadoPerfilEstudiante(
                    profile.automaticStudentAccess()
                            ? PENDING_VERIFICATION
                            : "PENDIENTE_REVISION"
            );
            estudiante.setFechaCreacion(now);
            estudianteRepository.save(estudiante);
        }

        VerificacionUsuario verification = verificationRecordService.create(
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
                    if (PENDING_VERIFICATION.equals(
                            estudiante.getEstadoPerfilEstudiante()
                    )) {
                        estudiante.setEstadoPerfilEstudiante(ACTIVE_USER);
                        estudianteRepository.save(estudiante);
                    }
                });

        verification.setEstadoVerificacion(VERIFIED);
        verification.setFechaVerificacion(now);
        verification.setUltimaRevalidacion(now);
        verificacionRepository.save(verification);

        return authSessionService.createSession(usuario, ipOrigen, userAgent);
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

        VerificacionUsuario verification = verificationRecordService.create(
                usuario,
                now,
                REGISTRATION_VERIFICATION,
                verificationMinutes
        );
        emailService.sendCode(correo, verification.getCodigoVerificacion(), verificationMinutes);
        return registrationResponse(verification);
    }

    private void assignStudentRole(Integer userId) {
        int assignedStudentRole = usuarioRolRepository.assignRole(
                userId,
                "ESTUDIANTE"
        );
        if (assignedStudentRole == 0) {
            throw new IllegalStateException("No se encontro el rol ESTUDIANTE.");
        }
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

    private String normalizeEmail(String correo) {
        return correo == null ? "" : correo.trim().toLowerCase(Locale.ROOT);
    }

    private LocalDateTime now() {
        return LocalDateTime.now().truncatedTo(ChronoUnit.MICROS);
    }
}
