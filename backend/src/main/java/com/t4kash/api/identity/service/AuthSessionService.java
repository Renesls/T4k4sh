package com.t4kash.api.identity.service;

import com.t4kash.api.exception.InvalidCredentialsException;
import com.t4kash.api.identity.dto.AuthResponse;
import com.t4kash.api.identity.dto.AuthenticatedUserResponse;
import com.t4kash.api.identity.entity.SesionUsuario;
import com.t4kash.api.identity.entity.Universidad;
import com.t4kash.api.identity.repository.CarreraRepository;
import com.t4kash.api.identity.repository.SesionUsuarioRepository;
import com.t4kash.api.identity.repository.UniversidadRepository;
import com.t4kash.api.identity.repository.UsuarioRolRepository;
import com.t4kash.api.marketplace.entity.Usuario;
import com.t4kash.api.marketplace.entity.UsuarioEstudiante;
import com.t4kash.api.marketplace.repository.UsuarioEstudianteRepository;
import com.t4kash.api.marketplace.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Centraliza el ciclo de vida de las sesiones: emite tokens, identifica al
 * usuario mediante un token bearer y cierra sesiones. RegistrationService y
 * LoginService crean la sesion al verificar al usuario, mientras que
 * AuthenticatedUserService consulta al usuario en cada solicitud protegida.
 */
@Service
public class AuthSessionService {
    private static final String ACTIVE_USER = "ACTIVO";
    private static final String ACTIVE_SESSION = "ACTIVA";
    private static final int SESSION_DAYS = 7;

    private final UsuarioRepository usuarioRepository;
    private final UsuarioEstudianteRepository estudianteRepository;
    private final UsuarioRolRepository usuarioRolRepository;
    private final SesionUsuarioRepository sesionRepository;
    private final UniversidadRepository universidadRepository;
    private final CarreraRepository carreraRepository;
    private final SessionTokenService tokenService;
    private final AdminEmailPolicyService adminEmailPolicyService;

    public AuthSessionService(
            UsuarioRepository usuarioRepository,
            UsuarioEstudianteRepository estudianteRepository,
            UsuarioRolRepository usuarioRolRepository,
            SesionUsuarioRepository sesionRepository,
            UniversidadRepository universidadRepository,
            CarreraRepository carreraRepository,
            SessionTokenService tokenService,
            AdminEmailPolicyService adminEmailPolicyService
    ) {
        this.usuarioRepository = usuarioRepository;
        this.estudianteRepository = estudianteRepository;
        this.usuarioRolRepository = usuarioRolRepository;
        this.sesionRepository = sesionRepository;
        this.universidadRepository = universidadRepository;
        this.carreraRepository = carreraRepository;
        this.tokenService = tokenService;
        this.adminEmailPolicyService = adminEmailPolicyService;
    }

    public AuthResponse createSession(
            Usuario usuario,
            String ipOrigen,
            String userAgent
    ) {
        assignConfiguredAdminRole(usuario);
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

    public void closeActiveSessions(Integer userId, LocalDateTime now) {
        sesionRepository.findAllByIdUsuarioAndEstadoSesion(userId, ACTIVE_SESSION)
                .forEach(session -> {
                    session.setEstadoSesion("CERRADA");
                    session.setFechaCierre(now);
                    sesionRepository.save(session);
                });
    }

    private void assignConfiguredAdminRole(Usuario usuario) {
        if (adminEmailPolicyService.isAdmin(usuario.getCorreo())) {
            usuarioRolRepository.assignRole(usuario.getIdUsuario(), "ADMIN");
        }
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
