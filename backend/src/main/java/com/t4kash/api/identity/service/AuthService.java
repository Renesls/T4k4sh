package com.t4kash.api.identity.service;

import com.t4kash.api.exception.InvalidCredentialsException;
import com.t4kash.api.exception.ResourceConflictException;
import com.t4kash.api.identity.dto.AuthResponse;
import com.t4kash.api.identity.dto.AuthenticatedUserResponse;
import com.t4kash.api.identity.dto.LoginRequest;
import com.t4kash.api.identity.dto.RegisterRequest;
import com.t4kash.api.identity.entity.SesionUsuario;
import com.t4kash.api.identity.repository.SesionUsuarioRepository;
import com.t4kash.api.identity.repository.UsuarioRolRepository;
import com.t4kash.api.marketplace.entity.Usuario;
import com.t4kash.api.marketplace.entity.UsuarioEstudiante;
import com.t4kash.api.marketplace.repository.UsuarioEstudianteRepository;
import com.t4kash.api.marketplace.repository.UsuarioRepository;
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
    private static final String ACTIVE_SESSION = "ACTIVA";
    private static final int SESSION_DAYS = 7;

    private final UsuarioRepository usuarioRepository;
    private final UsuarioEstudianteRepository estudianteRepository;
    private final UsuarioRolRepository usuarioRolRepository;
    private final SesionUsuarioRepository sesionRepository;
    private final SessionTokenService tokenService;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(12);

    public AuthService(
            UsuarioRepository usuarioRepository,
            UsuarioEstudianteRepository estudianteRepository,
            UsuarioRolRepository usuarioRolRepository,
            SesionUsuarioRepository sesionRepository,
            SessionTokenService tokenService
    ) {
        this.usuarioRepository = usuarioRepository;
        this.estudianteRepository = estudianteRepository;
        this.usuarioRolRepository = usuarioRolRepository;
        this.sesionRepository = sesionRepository;
        this.tokenService = tokenService;
    }

    @Transactional
    public AuthResponse register(
            RegisterRequest request,
            String ipOrigen,
            String userAgent
    ) {
        String correo = normalizeEmail(request.correo());
        if (usuarioRepository.existsByCorreoIgnoreCase(correo)) {
            throw new ResourceConflictException("Ya existe una cuenta con ese correo.");
        }

        Usuario usuario = new Usuario();
        usuario.setNombre(request.nombre().trim());
        usuario.setApellido(request.apellido().trim());
        usuario.setCorreo(correo);
        usuario.setPasswordHash(passwordEncoder.encode(request.password()));
        usuario.setFechaRegistro(LocalDateTime.now().truncatedTo(ChronoUnit.MICROS));
        usuario.setEstadoUsuario(ACTIVE_USER);
        usuario = usuarioRepository.save(usuario);

        int assignedRoles = usuarioRolRepository.assignMarketplaceRoles(usuario.getIdUsuario());
        if (assignedRoles == 0) {
            throw new IllegalStateException("No se encontraron los roles base del marketplace.");
        }

        UsuarioEstudiante estudiante = new UsuarioEstudiante();
        estudiante.setIdUsuario(usuario.getIdUsuario());
        estudiante.setEstadoPerfilEstudiante("INCOMPLETO");
        estudiante.setFechaCreacion(LocalDateTime.now().truncatedTo(ChronoUnit.MICROS));
        estudianteRepository.save(estudiante);

        return createSession(usuario, ipOrigen, userAgent);
    }

    @Transactional
    public AuthResponse login(
            LoginRequest request,
            String ipOrigen,
            String userAgent
    ) {
        Usuario usuario = usuarioRepository.findByCorreoIgnoreCase(normalizeEmail(request.correo()))
                .orElseThrow(() -> new InvalidCredentialsException("Correo o contrasena incorrectos."));

        if (!ACTIVE_USER.equalsIgnoreCase(usuario.getEstadoUsuario())
                || !passwordEncoder.matches(request.password(), usuario.getPasswordHash())) {
            throw new InvalidCredentialsException("Correo o contrasena incorrectos.");
        }

        return createSession(usuario, ipOrigen, userAgent);
    }

    @Transactional
    public AuthenticatedUserResponse getCurrentUser(String rawToken) {
        SesionUsuario sesion = requireActiveSession(rawToken);
        Usuario usuario = usuarioRepository.findById(sesion.getIdUsuario())
                .orElseThrow(() -> new InvalidCredentialsException("La cuenta de la sesion ya no existe."));
        return toUserResponse(usuario);
    }

    @Transactional
    public void logout(String rawToken) {
        SesionUsuario sesion = requireActiveSession(rawToken);
        sesion.setEstadoSesion("CERRADA");
        sesion.setFechaCierre(LocalDateTime.now().truncatedTo(ChronoUnit.MICROS));
        sesionRepository.save(sesion);
    }

    private AuthResponse createSession(
            Usuario usuario,
            String ipOrigen,
            String userAgent
    ) {
        String rawToken = tokenService.generateToken();
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.MICROS);

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
                .orElseThrow(() -> new InvalidCredentialsException("La sesion no es valida."));

        if (!sesion.getFechaExpiracion().isAfter(LocalDateTime.now())) {
            sesion.setEstadoSesion("EXPIRADA");
            sesion.setFechaCierre(LocalDateTime.now().truncatedTo(ChronoUnit.MICROS));
            sesionRepository.save(sesion);
            throw new InvalidCredentialsException("La sesion ha expirado.");
        }
        return sesion;
    }

    private AuthenticatedUserResponse toUserResponse(Usuario usuario) {
        return new AuthenticatedUserResponse(
                usuario.getIdUsuario(),
                usuario.getNombre(),
                usuario.getApellido(),
                usuario.getCorreo(),
                usuario.getEstadoUsuario(),
                usuarioRolRepository.findRoleNames(usuario.getIdUsuario())
        );
    }

    private String normalizeEmail(String correo) {
        return correo.trim().toLowerCase(Locale.ROOT);
    }

    private String limit(String value, int maxLength) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
