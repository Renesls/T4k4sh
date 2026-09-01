package com.t4kash.api.network.service;

import com.t4kash.api.communication.service.NotificationService;
import com.t4kash.api.config.PaginationSupport;
import com.t4kash.api.exception.ForbiddenOperationException;
import com.t4kash.api.exception.ResourceConflictException;
import com.t4kash.api.exception.ResourceNotFoundException;
import com.t4kash.api.identity.dto.PublicIdentityResponse;
import com.t4kash.api.identity.service.PublicProfileService;
import com.t4kash.api.marketplace.entity.Usuario;
import com.t4kash.api.marketplace.repository.UsuarioRepository;
import com.t4kash.api.network.dto.CommentResponse;
import com.t4kash.api.network.dto.CreateCommentRequest;
import com.t4kash.api.network.dto.CreatePostRequest;
import com.t4kash.api.network.dto.PostResponse;
import com.t4kash.api.network.dto.UpdateCommentRequest;
import com.t4kash.api.network.dto.UpdatePostRequest;
import com.t4kash.api.network.entity.ComentarioPublicacion;
import com.t4kash.api.network.entity.Publicacion;
import com.t4kash.api.network.entity.PublicacionGuardada;
import com.t4kash.api.network.entity.PublicacionGuardadaId;
import com.t4kash.api.network.entity.ReaccionPublicacion;
import com.t4kash.api.network.repository.ComentarioPublicacionRepository;
import com.t4kash.api.network.repository.PublicacionGuardadaRepository;
import com.t4kash.api.network.repository.PublicacionRepository;
import com.t4kash.api.network.repository.PublicationCountProjection;
import com.t4kash.api.network.repository.ReaccionPublicacionRepository;
import com.t4kash.api.network.repository.ReactionCountProjection;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class NetworkService {
    private static final String ACTIVE_POST = "ACTIVA";
    private static final String DELETED_POST = "ELIMINADA";
    private static final String ACTIVE_COMMENT = "ACTIVO";
    private static final String DELETED_COMMENT = "ELIMINADO";
    private static final String SHARED_POST = "COMPARTIDA";

    private static final Set<String> POST_TYPES = Set.of(
            "TEXTO",
            "IMAGEN",
            "VIDEO",
            "PROYECTO",
            "LOGRO",
            "PREGUNTA",
            "RECURSO",
            "EVENTO",
            SHARED_POST
    );
    private static final Set<String> VISIBILITIES = Set.of(
            "PUBLICA",
            "CONEXIONES",
            "UNIVERSIDAD"
    );
    private static final Set<String> FEED_SCOPES = Set.of(
            "PARA_TI",
            "CONEXIONES",
            "UNIVERSIDAD"
    );
    private static final Set<String> REACTIONS = Set.of(
            "ME_GUSTA",
            "APOYO",
            "CELEBRAR",
            "INTERESA"
    );

    private final PublicacionRepository publicationRepository;
    private final ComentarioPublicacionRepository commentRepository;
    private final ReaccionPublicacionRepository reactionRepository;
    private final PublicacionGuardadaRepository savedRepository;
    private final UsuarioRepository userRepository;
    private final PublicProfileService profileService;
    private final NotificationService notificationService;

    public NetworkService(
            PublicacionRepository publicationRepository,
            ComentarioPublicacionRepository commentRepository,
            ReaccionPublicacionRepository reactionRepository,
            PublicacionGuardadaRepository savedRepository,
            UsuarioRepository userRepository,
            PublicProfileService profileService,
            NotificationService notificationService
    ) {
        this.publicationRepository = publicationRepository;
        this.commentRepository = commentRepository;
        this.reactionRepository = reactionRepository;
        this.savedRepository = savedRepository;
        this.userRepository = userRepository;
        this.profileService = profileService;
        this.notificationService = notificationService;
    }

    @Transactional(readOnly = true)
    public List<PostResponse> listFeed(
            Integer currentUserId,
            String requestedScope,
            int page,
            int size
    ) {
        String scope = normalizedRequired(requestedScope, "alcance");
        requireAllowed(scope, FEED_SCOPES, "El alcance del feed no es valido.");
        return toPostResponses(
                publicationRepository.findFeed(
                        currentUserId,
                        scope,
                        PaginationSupport.page(page, size)
                ),
                currentUserId
        );
    }

    @Transactional(readOnly = true)
    public List<PostResponse> listSaved(
            Integer currentUserId,
            int page,
            int size
    ) {
        return toPostResponses(
                publicationRepository.findSaved(
                        currentUserId,
                        PaginationSupport.page(page, size)
                ),
                currentUserId
        );
    }

    @Transactional(readOnly = true)
    public PostResponse getPost(Integer currentUserId, Integer publicationId) {
        return toPostResponse(requireVisiblePost(currentUserId, publicationId), currentUserId);
    }

    @Transactional
    public PostResponse createPost(
            Integer currentUserId,
            CreatePostRequest request
    ) {
        Usuario user = requireUser(currentUserId);
        String type = normalizePostType(request.tipoPublicacion());
        String visibility = normalizeVisibility(request.visibilidad(), user);
        String content = normalizeContent(request.contenido());
        Publicacion source = validateSource(
                currentUserId,
                type,
                request.idPublicacionOrigen()
        );
        validateContent(content, source);

        Publicacion publication = new Publicacion();
        publication.setIdUsuario(currentUserId);
        publication.setIdPublicacionOrigen(
                source == null ? null : source.getIdPublicacion()
        );
        publication.setContenido(content);
        publication.setTipoPublicacion(type);
        publication.setVisibilidad(visibility);
        publication.setPermiteComentarios(
                request.permiteComentarios() == null
                        || request.permiteComentarios()
        );
        publication.setFechaPublicacion(now());
        publication.setFechaEdicion(null);
        publication.setEstadoPublicacion(ACTIVE_POST);
        Publicacion saved = publicationRepository.save(publication);

        if (source != null && !source.getIdUsuario().equals(currentUserId)) {
            PublicIdentityResponse actor = profileService.getIdentity(currentUserId);
            notificationService.create(
                    source.getIdUsuario(),
                    "Compartieron tu publicacion",
                    actor.nombreCompleto() + " compartio una de tus publicaciones."
            );
        }
        return toPostResponse(saved, currentUserId);
    }

    @Transactional
    public PostResponse updatePost(
            Integer currentUserId,
            Integer publicationId,
            UpdatePostRequest request
    ) {
        Publicacion publication = requireOwnedActivePost(currentUserId, publicationId);
        Usuario user = requireUser(currentUserId);
        String type = normalizePostType(request.tipoPublicacion());
        boolean shared = publication.getIdPublicacionOrigen() != null;
        if (shared != SHARED_POST.equals(type)) {
            throw new IllegalArgumentException(
                    "El tipo de una publicacion compartida no puede cambiar."
            );
        }
        String content = normalizeContent(request.contenido());
        validateContent(content, shared ? publication : null);

        publication.setContenido(content);
        publication.setTipoPublicacion(type);
        publication.setVisibilidad(normalizeVisibility(request.visibilidad(), user));
        publication.setPermiteComentarios(
                request.permiteComentarios() == null
                        ? publication.isPermiteComentarios()
                        : request.permiteComentarios()
        );
        publication.setFechaEdicion(now());
        return toPostResponse(publicationRepository.save(publication), currentUserId);
    }

    @Transactional
    public PostResponse deletePost(Integer currentUserId, Integer publicationId) {
        Publicacion publication = requireOwnedActivePost(currentUserId, publicationId);
        publication.setEstadoPublicacion(DELETED_POST);
        publication.setFechaEdicion(now());
        return toPostResponse(publicationRepository.save(publication), currentUserId);
    }

    @Transactional
    public PostResponse setReaction(
            Integer currentUserId,
            Integer publicationId,
            String requestedReaction
    ) {
        Publicacion publication = requireVisiblePost(currentUserId, publicationId);
        String reactionType = normalizedRequired(requestedReaction, "reaccion");
        requireAllowed(
                reactionType,
                REACTIONS,
                "La reaccion seleccionada no es valida."
        );

        ReaccionPublicacion reaction = reactionRepository
                .findByIdPublicacionAndIdUsuario(publicationId, currentUserId)
                .orElse(null);
        boolean isNew = reaction == null;
        if (isNew) {
            reaction = new ReaccionPublicacion();
            reaction.setIdPublicacion(publicationId);
            reaction.setIdUsuario(currentUserId);
            reaction.setFechaReaccion(now());
        }
        reaction.setTipoReaccion(reactionType);
        reactionRepository.save(reaction);

        if (isNew && !publication.getIdUsuario().equals(currentUserId)) {
            PublicIdentityResponse actor = profileService.getIdentity(currentUserId);
            notificationService.create(
                    publication.getIdUsuario(),
                    "Nueva reaccion",
                    actor.nombreCompleto() + " reacciono a tu publicacion."
            );
        }
        return toPostResponse(publication, currentUserId);
    }

    @Transactional
    public PostResponse removeReaction(
            Integer currentUserId,
            Integer publicationId
    ) {
        Publicacion publication = requireVisiblePost(currentUserId, publicationId);
        reactionRepository.deleteByIdPublicacionAndIdUsuario(
                publicationId,
                currentUserId
        );
        reactionRepository.flush();
        return toPostResponse(publication, currentUserId);
    }

    @Transactional
    public PostResponse savePost(Integer currentUserId, Integer publicationId) {
        Publicacion publication = requireVisiblePost(currentUserId, publicationId);
        PublicacionGuardadaId id = new PublicacionGuardadaId(
                publicationId,
                currentUserId
        );
        if (!savedRepository.existsById(id)) {
            PublicacionGuardada saved = new PublicacionGuardada();
            saved.setIdPublicacion(publicationId);
            saved.setIdUsuario(currentUserId);
            saved.setFechaGuardado(now());
            savedRepository.save(saved);
        }
        return toPostResponse(publication, currentUserId);
    }

    @Transactional
    public PostResponse removeSavedPost(
            Integer currentUserId,
            Integer publicationId
    ) {
        Publicacion publication = requireVisiblePost(currentUserId, publicationId);
        savedRepository.deleteByIdPublicacionAndIdUsuario(
                publicationId,
                currentUserId
        );
        savedRepository.flush();
        return toPostResponse(publication, currentUserId);
    }

    @Transactional(readOnly = true)
    public List<CommentResponse> listComments(
            Integer currentUserId,
            Integer publicationId,
            int page,
            int size
    ) {
        requireVisiblePost(currentUserId, publicationId);
        List<ComentarioPublicacion> comments = commentRepository.findVisibleComments(
                currentUserId,
                publicationId,
                PaginationSupport.page(page, size)
        );
        Map<Integer, PublicIdentityResponse> identities = profileService.getIdentities(
                comments.stream().map(ComentarioPublicacion::getIdUsuario).toList()
        );
        return comments.stream()
                .map(comment -> toCommentResponse(
                        comment,
                        identities.get(comment.getIdUsuario()),
                        currentUserId
                ))
                .toList();
    }

    @Transactional
    public CommentResponse createComment(
            Integer currentUserId,
            Integer publicationId,
            CreateCommentRequest request
    ) {
        Publicacion publication = requireVisiblePost(currentUserId, publicationId);
        if (!publication.isPermiteComentarios()) {
            throw new ResourceConflictException(
                    "Esta publicacion no admite comentarios."
            );
        }

        ComentarioPublicacion parent = requireValidParent(
                publicationId,
                request.idComentarioPadre()
        );
        ComentarioPublicacion comment = new ComentarioPublicacion();
        comment.setIdPublicacion(publicationId);
        comment.setIdUsuario(currentUserId);
        comment.setIdComentarioPadre(
                parent == null ? null : parent.getIdComentarioPublicacion()
        );
        comment.setContenido(request.contenido().trim());
        comment.setFechaComentario(now());
        comment.setFechaEdicion(null);
        comment.setEstadoComentario(ACTIVE_COMMENT);
        ComentarioPublicacion saved = commentRepository.save(comment);

        PublicIdentityResponse actor = profileService.getIdentity(currentUserId);
        notifyCommentRecipients(publication, parent, actor);
        return toCommentResponse(saved, actor, currentUserId);
    }

    @Transactional
    public CommentResponse updateComment(
            Integer currentUserId,
            Integer commentId,
            UpdateCommentRequest request
    ) {
        ComentarioPublicacion comment = requireOwnedActiveComment(
                currentUserId,
                commentId
        );
        requireVisiblePost(currentUserId, comment.getIdPublicacion());
        comment.setContenido(request.contenido().trim());
        comment.setFechaEdicion(now());
        ComentarioPublicacion saved = commentRepository.save(comment);
        return toCommentResponse(
                saved,
                profileService.getIdentity(currentUserId),
                currentUserId
        );
    }

    @Transactional
    public CommentResponse deleteComment(
            Integer currentUserId,
            Integer commentId
    ) {
        ComentarioPublicacion comment = requireOwnedActiveComment(
                currentUserId,
                commentId
        );
        comment.setEstadoComentario(DELETED_COMMENT);
        comment.setFechaEdicion(now());
        ComentarioPublicacion saved = commentRepository.save(comment);
        return toCommentResponse(
                saved,
                profileService.getIdentity(currentUserId),
                currentUserId
        );
    }

    private List<PostResponse> toPostResponses(
            List<Publicacion> publications,
            Integer currentUserId
    ) {
        if (publications.isEmpty()) {
            return List.of();
        }
        List<Integer> ids = publications.stream()
                .map(Publicacion::getIdPublicacion)
                .toList();
        Map<Integer, PublicIdentityResponse> identities = profileService.getIdentities(
                publications.stream().map(Publicacion::getIdUsuario).toList()
        );
        Map<Integer, Long> commentCounts = countsByPublication(
                commentRepository.countByPublicationIds(ids)
        );
        Map<Integer, Long> shareCounts = countsByPublication(
                publicationRepository.countSharesByPublicationIds(ids)
        );
        Map<Integer, Map<String, Long>> reactionCounts = reactionCounts(
                reactionRepository.countByPublicationIds(ids)
        );
        Map<Integer, String> myReactions = reactionRepository
                .findByIdUsuarioAndIdPublicacionIn(currentUserId, ids)
                .stream()
                .collect(Collectors.toMap(
                        ReaccionPublicacion::getIdPublicacion,
                        ReaccionPublicacion::getTipoReaccion
                ));
        Set<Integer> savedIds = savedRepository
                .findByIdUsuarioAndIdPublicacionIn(currentUserId, ids)
                .stream()
                .map(PublicacionGuardada::getIdPublicacion)
                .collect(Collectors.toSet());

        return publications.stream()
                .map(publication -> toPostResponse(
                        publication,
                        identities.get(publication.getIdUsuario()),
                        reactionCounts.getOrDefault(
                                publication.getIdPublicacion(),
                                Map.of()
                        ),
                        commentCounts.getOrDefault(publication.getIdPublicacion(), 0L),
                        shareCounts.getOrDefault(publication.getIdPublicacion(), 0L),
                        myReactions.get(publication.getIdPublicacion()),
                        savedIds.contains(publication.getIdPublicacion()),
                        currentUserId
                ))
                .toList();
    }

    private PostResponse toPostResponse(
            Publicacion publication,
            Integer currentUserId
    ) {
        return toPostResponses(List.of(publication), currentUserId).getFirst();
    }

    private PostResponse toPostResponse(
            Publicacion publication,
            PublicIdentityResponse author,
            Map<String, Long> reactionCounts,
            long commentCount,
            long shareCount,
            String myReaction,
            boolean saved,
            Integer currentUserId
    ) {
        Map<String, Long> orderedReactions = new LinkedHashMap<>();
        REACTIONS.stream().sorted().forEach(type -> {
            long total = reactionCounts.getOrDefault(type, 0L);
            if (total > 0) {
                orderedReactions.put(type, total);
            }
        });
        return new PostResponse(
                publication.getIdPublicacion(),
                author,
                publication.getIdPublicacionOrigen(),
                publication.getContenido(),
                publication.getTipoPublicacion(),
                publication.getVisibilidad(),
                publication.isPermiteComentarios(),
                publication.getFechaPublicacion(),
                publication.getFechaEdicion(),
                publication.getEstadoPublicacion(),
                orderedReactions,
                orderedReactions.values().stream().mapToLong(Long::longValue).sum(),
                commentCount,
                shareCount,
                myReaction,
                saved,
                publication.getIdUsuario().equals(currentUserId)
        );
    }

    private CommentResponse toCommentResponse(
            ComentarioPublicacion comment,
            PublicIdentityResponse author,
            Integer currentUserId
    ) {
        return new CommentResponse(
                comment.getIdComentarioPublicacion(),
                comment.getIdPublicacion(),
                comment.getIdComentarioPadre(),
                author,
                comment.getContenido(),
                comment.getFechaComentario(),
                comment.getFechaEdicion(),
                comment.getIdUsuario().equals(currentUserId)
        );
    }

    private Publicacion requireVisiblePost(
            Integer currentUserId,
            Integer publicationId
    ) {
        return publicationRepository.findVisibleById(currentUserId, publicationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "La publicacion indicada no existe o no esta disponible."
                ));
    }

    private Publicacion requireOwnedActivePost(
            Integer currentUserId,
            Integer publicationId
    ) {
        Publicacion publication = publicationRepository.findById(publicationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "La publicacion indicada no existe."
                ));
        if (!publication.getIdUsuario().equals(currentUserId)) {
            throw new ForbiddenOperationException(
                    "No puedes modificar publicaciones de otra cuenta."
            );
        }
        if (!ACTIVE_POST.equals(publication.getEstadoPublicacion())) {
            throw new ResourceConflictException(
                    "Solo se pueden modificar publicaciones activas."
            );
        }
        return publication;
    }

    private ComentarioPublicacion requireOwnedActiveComment(
            Integer currentUserId,
            Integer commentId
    ) {
        ComentarioPublicacion comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "El comentario indicado no existe."
                ));
        if (!comment.getIdUsuario().equals(currentUserId)) {
            throw new ForbiddenOperationException(
                    "No puedes modificar comentarios de otra cuenta."
            );
        }
        if (!ACTIVE_COMMENT.equals(comment.getEstadoComentario())) {
            throw new ResourceConflictException(
                    "Solo se pueden modificar comentarios activos."
            );
        }
        return comment;
    }

    private ComentarioPublicacion requireValidParent(
            Integer publicationId,
            Integer parentId
    ) {
        if (parentId == null) {
            return null;
        }
        ComentarioPublicacion parent = commentRepository.findById(parentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "El comentario al que intentas responder no existe."
                ));
        if (!publicationId.equals(parent.getIdPublicacion())
                || !ACTIVE_COMMENT.equals(parent.getEstadoComentario())) {
            throw new IllegalArgumentException(
                    "La respuesta debe pertenecer a un comentario activo de esta publicacion."
            );
        }
        return parent;
    }

    private Publicacion validateSource(
            Integer currentUserId,
            String type,
            Integer sourceId
    ) {
        if (!SHARED_POST.equals(type)) {
            if (sourceId != null) {
                throw new IllegalArgumentException(
                        "Solo una publicacion compartida puede indicar una publicacion de origen."
                );
            }
            return null;
        }
        if (sourceId == null) {
            throw new IllegalArgumentException(
                    "Selecciona la publicacion que deseas compartir."
            );
        }
        return requireVisiblePost(currentUserId, sourceId);
    }

    private void validateContent(String content, Publicacion source) {
        if ((content == null || content.isBlank()) && source == null) {
            throw new IllegalArgumentException(
                    "Escribe el contenido de la publicacion."
            );
        }
    }

    private String normalizePostType(String value) {
        String normalized = normalizedRequired(value, "tipo de publicacion");
        requireAllowed(
                normalized,
                POST_TYPES,
                "El tipo de publicacion no es valido."
        );
        return normalized;
    }

    private String normalizeVisibility(String value, Usuario user) {
        String normalized = normalizedRequired(value, "visibilidad");
        requireAllowed(
                normalized,
                VISIBILITIES,
                "La visibilidad de la publicacion no es valida."
        );
        if ("UNIVERSIDAD".equals(normalized) && user.getIdUniversidad() == null) {
            throw new IllegalArgumentException(
                    "Necesitas una universidad asociada para usar esta visibilidad."
            );
        }
        return normalized;
    }

    private String normalizeContent(String value) {
        if (value == null) {
            return null;
        }
        String clean = value.trim();
        return clean.isEmpty() ? null : clean;
    }

    private String normalizedRequired(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Selecciona " + field + ".");
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private void requireAllowed(
            String value,
            Set<String> allowed,
            String message
    ) {
        if (!allowed.contains(value)) {
            throw new IllegalArgumentException(message);
        }
    }

    private Usuario requireUser(Integer userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "El usuario indicado no existe."
                ));
    }

    private void notifyCommentRecipients(
            Publicacion publication,
            ComentarioPublicacion parent,
            PublicIdentityResponse actor
    ) {
        Set<Integer> recipients = new java.util.HashSet<>();
        recipients.add(publication.getIdUsuario());
        if (parent != null) {
            recipients.add(parent.getIdUsuario());
        }
        recipients.remove(actor.idUsuario());
        recipients.forEach(recipient -> notificationService.create(
                recipient,
                parent == null ? "Nuevo comentario" : "Nueva respuesta",
                actor.nombreCompleto() + " participo en una publicacion."
        ));
    }

    private Map<Integer, Long> countsByPublication(
            Collection<PublicationCountProjection> projections
    ) {
        return projections.stream().collect(Collectors.toMap(
                PublicationCountProjection::getIdPublicacion,
                PublicationCountProjection::getTotal
        ));
    }

    private Map<Integer, Map<String, Long>> reactionCounts(
            Collection<ReactionCountProjection> projections
    ) {
        Map<Integer, Map<String, Long>> counts = new HashMap<>();
        projections.forEach(item -> counts
                .computeIfAbsent(item.getIdPublicacion(), ignored -> new HashMap<>())
                .put(item.getTipoReaccion(), item.getTotal()));
        return counts;
    }

    private LocalDateTime now() {
        return LocalDateTime.now().truncatedTo(ChronoUnit.MICROS);
    }
}
