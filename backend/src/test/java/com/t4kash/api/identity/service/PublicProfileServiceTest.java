package com.t4kash.api.identity.service;

import com.t4kash.api.exception.ResourceConflictException;
import com.t4kash.api.identity.entity.HistorialNombreUsuario;
import com.t4kash.api.identity.repository.CarreraRepository;
import com.t4kash.api.identity.repository.HistorialNombreUsuarioRepository;
import com.t4kash.api.identity.repository.UniversidadRepository;
import com.t4kash.api.marketplace.entity.Usuario;
import com.t4kash.api.marketplace.repository.CalificacionRepository;
import com.t4kash.api.marketplace.repository.TareaRepository;
import com.t4kash.api.marketplace.repository.TrabajoAsignadoRepository;
import com.t4kash.api.marketplace.repository.UsuarioEstudianteRepository;
import com.t4kash.api.marketplace.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PublicProfileServiceTest {
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private UsuarioEstudianteRepository estudianteRepository;
    @Mock private UniversidadRepository universidadRepository;
    @Mock private CarreraRepository carreraRepository;
    @Mock private HistorialNombreUsuarioRepository historialRepository;
    @Mock private TareaRepository tareaRepository;
    @Mock private TrabajoAsignadoRepository trabajoRepository;
    @Mock private CalificacionRepository calificacionRepository;

    private PublicProfileService service;

    @BeforeEach
    void setUp() {
        service = new PublicProfileService(
                usuarioRepository,
                estudianteRepository,
                universidadRepository,
                carreraRepository,
                historialRepository,
                tareaRepository,
                trabajoRepository,
                calificacionRepository
        );
    }

    @Test
    void updatesUsernameAndCreatesHistory() {
        Usuario user = user();
        when(usuarioRepository.findById(7)).thenReturn(Optional.of(user));
        when(historialRepository.findFirstByIdUsuarioOrderByFechaCambioDesc(7))
                .thenReturn(Optional.empty());
        when(usuarioRepository.findAllById(any())).thenReturn(List.of(user));
        when(estudianteRepository.findAllById(any())).thenReturn(List.of());
        when(universidadRepository.findAllById(any())).thenReturn(List.of());
        when(carreraRepository.findAllById(any())).thenReturn(List.of());
        when(calificacionRepository.findTop5ByIdCalificadoOrderByFechaCalificacionDesc(7))
                .thenReturn(List.of());

        var response = service.updateUsername(7, "@rene.dev");

        assertEquals("rene.dev", response.identidad().nombreUsuario());
        assertNotNull(response.proximoCambioNombreUsuario());
        ArgumentCaptor<HistorialNombreUsuario> history =
                ArgumentCaptor.forClass(HistorialNombreUsuario.class);
        verify(historialRepository).save(history.capture());
        assertEquals("rene.sandoval", history.getValue().getNombreAnterior());
        assertEquals("rene.dev", history.getValue().getNombreNuevo());
    }

    @Test
    void rejectsUsernameChangeDuringCooldown() {
        Usuario user = user();
        HistorialNombreUsuario history = new HistorialNombreUsuario();
        history.setFechaCambio(LocalDateTime.now().minusDays(2));
        when(usuarioRepository.findById(7)).thenReturn(Optional.of(user));
        when(historialRepository.findFirstByIdUsuarioOrderByFechaCambioDesc(7))
                .thenReturn(Optional.of(history));

        assertThrows(
                ResourceConflictException.class,
                () -> service.updateUsername(7, "rene.dev")
        );
    }

    @Test
    void keepsCurrentUsernameWithoutCreatingHistory() {
        Usuario user = user();
        HistorialNombreUsuario history = new HistorialNombreUsuario();
        history.setFechaCambio(LocalDateTime.now().minusDays(2));
        when(usuarioRepository.findById(7)).thenReturn(Optional.of(user));
        when(historialRepository.findFirstByIdUsuarioOrderByFechaCambioDesc(7))
                .thenReturn(Optional.of(history));
        when(usuarioRepository.findAllById(any())).thenReturn(List.of(user));
        when(estudianteRepository.findAllById(any())).thenReturn(List.of());
        when(universidadRepository.findAllById(any())).thenReturn(List.of());
        when(carreraRepository.findAllById(any())).thenReturn(List.of());
        when(calificacionRepository.findTop5ByIdCalificadoOrderByFechaCalificacionDesc(7))
                .thenReturn(List.of());

        var response = service.updateUsername(7, "@rene.sandoval");

        assertEquals("rene.sandoval", response.identidad().nombreUsuario());
        assertNotNull(response.proximoCambioNombreUsuario());
        verify(historialRepository, never()).save(any());
    }

    private Usuario user() {
        Usuario user = new Usuario();
        user.setIdUsuario(7);
        user.setNombre("Rene");
        user.setApellido("Sandoval");
        user.setNombreUsuario("rene.sandoval");
        user.setEstadoUsuario("ACTIVO");
        user.setFechaRegistro(LocalDateTime.now().minusMonths(2));
        return user;
    }
}
