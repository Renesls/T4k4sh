package com.t4kash.api.network.service;

import com.t4kash.api.communication.service.NotificationService;
import com.t4kash.api.exception.ForbiddenOperationException;
import com.t4kash.api.exception.ResourceConflictException;
import com.t4kash.api.identity.dto.PublicIdentityResponse;
import com.t4kash.api.identity.service.PublicProfileService;
import com.t4kash.api.marketplace.entity.Usuario;
import com.t4kash.api.marketplace.repository.UsuarioRepository;
import com.t4kash.api.network.dto.CreateCommentRequest;
import com.t4kash.api.network.dto.CreatePostRequest;
import com.t4kash.api.network.dto.PostResponse;
import com.t4kash.api.network.dto.UpdatePostRequest;
import com.t4kash.api.network.entity.Publicacion;
import com.t4kash.api.network.entity.ReaccionPublicacion;
import com.t4kash.api.network.repository.ComentarioPublicacionRepository;
import com.t4kash.api.network.repository.PublicacionGuardadaRepository;
import com.t4kash.api.network.repository.PublicacionRepository;
import com.t4kash.api.network.repository.ReaccionPublicacionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NetworkServiceTest {
    @Mock
    private PublicacionRepository publicationRepository;
    @Mock
    private ComentarioPublicacionRepository commentRepository;
    @Mock
    private ReaccionPublicacionRepository reactionRepository;
    @Mock
    private PublicacionGuardadaRepository savedRepository;
    @Mock
    private UsuarioRepository userRepository;
    @Mock
    private PublicProfileService profileService;
    @Mock
    private NotificationService notificationService;

    private NetworkService service;

    @BeforeEach
    void setUp() {
        service = new NetworkService(
                publicationRepository,
                commentRepository,
                reactionRepository,
                savedRepository,
                userRepository,
                profileService,
                notificationService
        );
        lenient().when(commentRepository.countByPublicationIds(anyCollection()))
                .thenReturn(List.of());
        lenient().when(publicationRepository.countSharesByPublicationIds(anyCollection()))
                .thenReturn(List.of());
        lenient().when(reactionRepository.countByPublicationIds(anyCollection()))
                .thenReturn(List.of());
        lenient().when(reactionRepository.findByIdUsuarioAndIdPublicacionIn(
                any(),
                anyCollection()
        )).thenReturn(List.of());
        lenient().when(savedRepository.findByIdUsuarioAndIdPublicacionIn(
                any(),
                anyCollection()
        )).thenReturn(List.of());
        lenient().when(profileService.getIdentities(anyCollection()))
                .thenAnswer(invocation -> {
                    List<Integer> ids = invocation.getArgument(0);
                    return ids.stream().distinct().collect(java.util.stream.Collectors.toMap(
                            value -> value,
                            this::identity
                    ));
                });
        lenient().when(profileService.getIdentity(any()))
                .thenAnswer(invocation -> identity(invocation.getArgument(0)));
        lenient().when(publicationRepository.save(any(Publicacion.class)))
                .thenAnswer(invocation -> {
                    Publicacion publication = invocation.getArgument(0);
                    if (publication.getIdPublicacion() == null) {
                        publication.setIdPublicacion(10);
                    }
                    return publication;
                });
    }

    @Test
    void createsNormalizedPublicPost() {
        when(userRepository.findById(1)).thenReturn(Optional.of(user(1, null)));

        PostResponse response = service.createPost(
                1,
                new CreatePostRequest(
                        "  Mi primer proyecto  ",
                        "texto",
                        "publica",
                        true,
                        null
                )
        );

        assertEquals("Mi primer proyecto", response.contenido());
        assertEquals("TEXTO", response.tipoPublicacion());
        assertEquals("PUBLICA", response.visibilidad());
        assertTrue(response.propia());
    }

    @Test
    void universityVisibilityRequiresAssociatedUniversity() {
        when(userRepository.findById(1)).thenReturn(Optional.of(user(1, null)));

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> service.createPost(
                        1,
                        new CreatePostRequest(
                                "Aviso para mi universidad",
                                "TEXTO",
                                "UNIVERSIDAD",
                                true,
                                null
                        )
                )
        );

        assertEquals(
                "Necesitas una universidad asociada para usar esta visibilidad.",
                error.getMessage()
        );
        verify(publicationRepository, never()).save(any());
    }

    @Test
    void sharedPostCanUseOnlyTheOriginalContent() {
        Publicacion source = post(8, 2, true);
        when(userRepository.findById(1)).thenReturn(Optional.of(user(1, 1)));
        when(publicationRepository.findVisibleById(1, 8))
                .thenReturn(Optional.of(source));

        PostResponse response = service.createPost(
                1,
                new CreatePostRequest(
                        null,
                        "COMPARTIDA",
                        "PUBLICA",
                        true,
                        8
                )
        );

        assertEquals(8, response.idPublicacionOrigen());
        assertNull(response.contenido());
        verify(notificationService).create(
                eq(2),
                eq("Compartieron tu publicacion"),
                any()
        );
    }

    @Test
    void anotherUserCannotEditThePost() {
        when(publicationRepository.findById(10))
                .thenReturn(Optional.of(post(10, 2, true)));

        assertThrows(
                ForbiddenOperationException.class,
                () -> service.updatePost(
                        1,
                        10,
                        new UpdatePostRequest(
                                "Contenido cambiado",
                                "TEXTO",
                                "PUBLICA",
                                true
                        )
                )
        );
    }

    @Test
    void changingAReactionDoesNotCreateAnotherOne() {
        Publicacion publication = post(10, 2, true);
        ReaccionPublicacion reaction = new ReaccionPublicacion();
        reaction.setIdReaccion(4);
        reaction.setIdPublicacion(10);
        reaction.setIdUsuario(1);
        reaction.setTipoReaccion("ME_GUSTA");
        when(publicationRepository.findVisibleById(1, 10))
                .thenReturn(Optional.of(publication));
        when(reactionRepository.findByIdPublicacionAndIdUsuario(10, 1))
                .thenReturn(Optional.of(reaction));

        service.setReaction(1, 10, "apoyo");

        assertEquals("APOYO", reaction.getTipoReaccion());
        verify(notificationService, never()).create(any(), any(), any());
    }

    @Test
    void closedPostRejectsComments() {
        Publicacion publication = post(10, 2, false);
        when(publicationRepository.findVisibleById(1, 10))
                .thenReturn(Optional.of(publication));

        ResourceConflictException error = assertThrows(
                ResourceConflictException.class,
                () -> service.createComment(
                        1,
                        10,
                        new CreateCommentRequest("Quiero participar", null)
                )
        );

        assertEquals("Esta publicacion no admite comentarios.", error.getMessage());
    }

    private Usuario user(Integer id, Integer universityId) {
        Usuario user = new Usuario();
        user.setIdUsuario(id);
        user.setIdUniversidad(universityId);
        user.setNombre("Usuario");
        user.setApellido(id.toString());
        user.setNombreUsuario("usuario." + id);
        user.setEstadoUsuario("ACTIVO");
        return user;
    }

    private Publicacion post(Integer id, Integer authorId, boolean commentsEnabled) {
        Publicacion publication = new Publicacion();
        publication.setIdPublicacion(id);
        publication.setIdUsuario(authorId);
        publication.setContenido("Contenido social");
        publication.setTipoPublicacion("TEXTO");
        publication.setVisibilidad("PUBLICA");
        publication.setPermiteComentarios(commentsEnabled);
        publication.setFechaPublicacion(java.time.LocalDateTime.now());
        publication.setEstadoPublicacion("ACTIVA");
        return publication;
    }

    private PublicIdentityResponse identity(Integer id) {
        return new PublicIdentityResponse(
                id,
                "usuario." + id,
                "Usuario " + id,
                id == 1 ? "Universidad Demo" : null,
                null,
                id == 1
        );
    }
}
