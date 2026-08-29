package com.t4kash.api.identity.service;

import com.t4kash.api.exception.ResourceConflictException;
import com.t4kash.api.exception.ResourceNotFoundException;
import com.t4kash.api.identity.dto.PublicIdentityResponse;
import com.t4kash.api.identity.dto.PublicProfileResponse;
import com.t4kash.api.identity.entity.Carrera;
import com.t4kash.api.identity.entity.HistorialNombreUsuario;
import com.t4kash.api.identity.entity.Universidad;
import com.t4kash.api.identity.repository.CarreraRepository;
import com.t4kash.api.identity.repository.HistorialNombreUsuarioRepository;
import com.t4kash.api.identity.repository.UniversidadRepository;
import com.t4kash.api.marketplace.entity.Usuario;
import com.t4kash.api.marketplace.entity.UsuarioEstudiante;
import com.t4kash.api.marketplace.repository.TareaRepository;
import com.t4kash.api.marketplace.repository.TrabajoAsignadoRepository;
import com.t4kash.api.marketplace.repository.UsuarioEstudianteRepository;
import com.t4kash.api.marketplace.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class PublicProfileService {
    private static final String ACTIVE = "ACTIVO";
    private static final int USERNAME_CHANGE_DAYS = 30;
    private static final Pattern USERNAME_PATTERN = Pattern.compile(
            "^[a-z0-9][a-z0-9._]{2,29}$"
    );

    private final UsuarioRepository usuarioRepository;
    private final UsuarioEstudianteRepository estudianteRepository;
    private final UniversidadRepository universidadRepository;
    private final CarreraRepository carreraRepository;
    private final HistorialNombreUsuarioRepository historialRepository;
    private final TareaRepository tareaRepository;
    private final TrabajoAsignadoRepository trabajoRepository;

    public PublicProfileService(
            UsuarioRepository usuarioRepository,
            UsuarioEstudianteRepository estudianteRepository,
            UniversidadRepository universidadRepository,
            CarreraRepository carreraRepository,
            HistorialNombreUsuarioRepository historialRepository,
            TareaRepository tareaRepository,
            TrabajoAsignadoRepository trabajoRepository
    ) {
        this.usuarioRepository = usuarioRepository;
        this.estudianteRepository = estudianteRepository;
        this.universidadRepository = universidadRepository;
        this.carreraRepository = carreraRepository;
        this.historialRepository = historialRepository;
        this.tareaRepository = tareaRepository;
        this.trabajoRepository = trabajoRepository;
    }

    @Transactional(readOnly = true)
    public PublicIdentityResponse getIdentity(Integer userId) {
        PublicIdentityResponse identity = getIdentities(List.of(userId)).get(userId);
        if (identity == null) {
            throw new ResourceNotFoundException("El perfil indicado no existe.");
        }
        return identity;
    }

    @Transactional(readOnly = true)
    public Map<Integer, PublicIdentityResponse> getIdentities(
            Collection<Integer> userIds
    ) {
        List<Integer> distinctIds = userIds.stream().distinct().toList();
        Map<Integer, Usuario> users = usuarioRepository.findAllById(distinctIds)
                .stream()
                .collect(Collectors.toMap(Usuario::getIdUsuario, Function.identity()));
        Map<Integer, UsuarioEstudiante> students = estudianteRepository
                .findAllById(distinctIds)
                .stream()
                .collect(Collectors.toMap(
                        UsuarioEstudiante::getIdUsuario,
                        Function.identity()
                ));
        Map<Integer, Universidad> universities = universidadRepository.findAllById(
                        users.values().stream()
                                .map(Usuario::getIdUniversidad)
                                .filter(value -> value != null)
                                .distinct()
                                .toList()
                ).stream()
                .collect(Collectors.toMap(
                        Universidad::getIdUniversidad,
                        Function.identity()
                ));
        Map<Integer, Carrera> careers = carreraRepository.findAllById(
                        students.values().stream()
                                .map(UsuarioEstudiante::getIdCarrera)
                                .filter(value -> value != null)
                                .distinct()
                                .toList()
                ).stream()
                .collect(Collectors.toMap(Carrera::getIdCarrera, Function.identity()));

        return users.values().stream().collect(Collectors.toMap(
                Usuario::getIdUsuario,
                user -> toIdentity(
                        user,
                        students.get(user.getIdUsuario()),
                        universities,
                        careers
                )
        ));
    }

    @Transactional(readOnly = true)
    public PublicProfileResponse getProfile(String requestedUsername) {
        String username = normalizeUsername(requestedUsername);
        Usuario user = usuarioRepository
                .findByNombreUsuarioIgnoreCaseAndEstadoUsuarioIgnoreCase(username, ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "El perfil indicado no existe."
                ));
        return toProfile(user, null);
    }

    @Transactional
    public PublicProfileResponse updateUsername(
            Integer userId,
            String requestedUsername
    ) {
        Usuario user = usuarioRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "El usuario indicado no existe."
                ));
        String username = normalizeUsername(requestedUsername);
        if (!USERNAME_PATTERN.matcher(username).matches()) {
            throw new IllegalArgumentException(
                    "Usa entre 3 y 30 caracteres: letras, numeros, puntos o guiones bajos."
            );
        }

        if (username.equalsIgnoreCase(user.getNombreUsuario())) {
            return toProfile(user, nextUsernameChange(userId));
        }

        LocalDateTime nextChange = nextUsernameChange(userId);
        if (nextChange != null && nextChange.isAfter(LocalDateTime.now())) {
            throw new ResourceConflictException(
                    "Podras cambiar tu nombre de usuario nuevamente el " + nextChange + "."
            );
        }
        if (usuarioRepository.existsByNombreUsuarioIgnoreCaseAndIdUsuarioNot(
                username,
                userId
        )) {
            throw new ResourceConflictException(
                    "Ese nombre de usuario ya esta en uso."
            );
        }

        String previousUsername = user.getNombreUsuario();
        user.setNombreUsuario(username);
        usuarioRepository.save(user);

        LocalDateTime changedAt = now();
        HistorialNombreUsuario history = new HistorialNombreUsuario();
        history.setIdUsuario(userId);
        history.setNombreAnterior(previousUsername);
        history.setNombreNuevo(username);
        history.setFechaCambio(changedAt);
        history.setMotivo("CAMBIO_SOLICITADO_USUARIO");
        historialRepository.save(history);
        return toProfile(user, changedAt.plusDays(USERNAME_CHANGE_DAYS));
    }

    private PublicProfileResponse toProfile(
            Usuario user,
            LocalDateTime nextChange
    ) {
        return new PublicProfileResponse(
                getIdentity(Math.toIntExact(user.getIdUsuario())),
                user.getFechaRegistro(),
                tareaRepository.countByIdCliente(user.getIdUsuario()),
                trabajoRepository.countByIdEstudianteAndEstadoTrabajo(
                        user.getIdUsuario(),
                        "FINALIZADO"
                ),
                nextChange
        );
    }

    private PublicIdentityResponse toIdentity(
            Usuario user,
            UsuarioEstudiante student,
            Map<Integer, Universidad> universities,
            Map<Integer, Carrera> careers
    ) {
        Universidad university = user.getIdUniversidad() == null
                ? null
                : universities.get(user.getIdUniversidad());
        Carrera career = student == null || student.getIdCarrera() == null
                ? null
                : careers.get(student.getIdCarrera());
        return new PublicIdentityResponse(
                Math.toIntExact(user.getIdUsuario()),
                user.getNombreUsuario(),
                (user.getNombre() + " " + user.getApellido()).trim(),
                university == null ? null : university.getNombreUniversidad(),
                career == null ? null : career.getNombreCarrera(),
                student != null && ACTIVE.equalsIgnoreCase(
                        student.getEstadoPerfilEstudiante()
                )
        );
    }

    private LocalDateTime nextUsernameChange(Integer userId) {
        return historialRepository.findFirstByIdUsuarioOrderByFechaCambioDesc(userId)
                .map(item -> item.getFechaCambio().plusDays(USERNAME_CHANGE_DAYS))
                .orElse(null);
    }

    private String normalizeUsername(String value) {
        String normalized = value == null
                ? ""
                : value.trim().toLowerCase(Locale.ROOT);
        return normalized.startsWith("@") ? normalized.substring(1) : normalized;
    }

    private LocalDateTime now() {
        return LocalDateTime.now().truncatedTo(ChronoUnit.MICROS);
    }
}