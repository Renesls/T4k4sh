package com.t4kash.api.communication.service;

import com.t4kash.api.communication.dto.ConversationResponse;
import com.t4kash.api.communication.dto.CreateMessageRequest;
import com.t4kash.api.communication.dto.MessageResponse;
import com.t4kash.api.communication.entity.Conversacion;
import com.t4kash.api.communication.entity.Mensaje;
import com.t4kash.api.communication.repository.ConversacionRepository;
import com.t4kash.api.communication.repository.MensajeRepository;
import com.t4kash.api.config.PaginationSupport;
import com.t4kash.api.exception.ForbiddenOperationException;
import com.t4kash.api.exception.ResourceConflictException;
import com.t4kash.api.exception.ResourceNotFoundException;
import com.t4kash.api.marketplace.entity.Postulacion;
import com.t4kash.api.marketplace.entity.Tarea;
import com.t4kash.api.marketplace.entity.TrabajoAsignado;
import com.t4kash.api.marketplace.entity.Usuario;
import com.t4kash.api.marketplace.repository.PostulacionRepository;
import com.t4kash.api.marketplace.repository.TareaRepository;
import com.t4kash.api.marketplace.repository.TrabajoAsignadoRepository;
import com.t4kash.api.marketplace.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ConversationService {
    private static final String CONVERSATION_OPEN = "ABIERTA";

    private final ConversacionRepository conversationRepository;
    private final MensajeRepository messageRepository;
    private final TareaRepository taskRepository;
    private final PostulacionRepository applicationRepository;
    private final TrabajoAsignadoRepository jobRepository;
    private final UsuarioRepository userRepository;
    private final NotificationService notificationService;

    public ConversationService(
            ConversacionRepository conversationRepository,
            MensajeRepository messageRepository,
            TareaRepository taskRepository,
            PostulacionRepository applicationRepository,
            TrabajoAsignadoRepository jobRepository,
            UsuarioRepository userRepository,
            NotificationService notificationService
    ) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.taskRepository = taskRepository;
        this.applicationRepository = applicationRepository;
        this.jobRepository = jobRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    @Transactional
    public Conversacion ensureForAcceptedApplication(
            Postulacion application,
            TrabajoAsignado job
    ) {
        return ensureForJob(job, application.getIdPostulacion());
    }

    @Transactional(readOnly = true)
    public List<ConversationResponse> listMine(Integer currentUserId) {
        return listMine(currentUserId, 0, PaginationSupport.DEFAULT_SIZE);
    }

    @Transactional(readOnly = true)
    public List<ConversationResponse> listMine(
            Integer currentUserId,
            int page,
            int size
    ) {
        List<Conversacion> conversations = conversationRepository
                .findVisibleToUser(
                        currentUserId,
                        PaginationSupport.page(page, size)
                );
        if (conversations.isEmpty()) {
            return List.of();
        }

        Map<Integer, Tarea> tasks = mapById(
                taskRepository.findAllById(ids(conversations, Conversacion::getIdTarea)),
                Tarea::getIdTarea
        );
        Map<Integer, TrabajoAsignado> jobs = mapById(
                jobRepository.findAllById(
                        nullableIds(conversations, Conversacion::getIdTrabajo)
                ),
                TrabajoAsignado::getIdTrabajo
        );
        Map<Integer, Postulacion> applications = mapById(
                applicationRepository.findAllById(
                        nullableIds(conversations, Conversacion::getIdPostulacion)
                ),
                Postulacion::getIdPostulacion
        );

        Set<Integer> participantIds = new HashSet<>();
        conversations.forEach(conversation -> {
            Tarea task = required(tasks, conversation.getIdTarea(), "oportunidad");
            participantIds.add(task.getIdCliente());
            participantIds.add(resolveStudentId(conversation, jobs, applications));
        });
        Map<Integer, Usuario> users = mapById(
                userRepository.findAllById(participantIds),
                Usuario::getIdUsuario
        );

        List<Integer> conversationIds = conversations.stream()
                .map(Conversacion::getIdConversacion)
                .toList();
        Map<Integer, Mensaje> latestMessages = mapById(
                messageRepository.findLatestByConversationIds(conversationIds),
                Mensaje::getIdConversacion
        );
        Map<Integer, Long> unreadCounts = messageRepository
                .countUnreadByConversationIds(conversationIds, currentUserId)
                .stream()
                .collect(Collectors.toMap(
                        item -> item.getIdConversacion(),
                        item -> item.getTotal()
                ));

        return conversations.stream()
                .map(conversation -> toResponse(
                        conversation,
                        currentUserId,
                        tasks,
                        jobs,
                        applications,
                        users,
                        latestMessages,
                        unreadCounts
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MessageResponse> listMessages(
            Integer currentUserId,
            Integer conversationId
    ) {
        return listMessages(
                currentUserId,
                conversationId,
                0,
                PaginationSupport.MAXIMUM_SIZE
        );
    }

    @Transactional(readOnly = true)
    public List<MessageResponse> listMessages(
            Integer currentUserId,
            Integer conversationId,
            int page,
            int size
    ) {
        Conversacion conversation = requireParticipant(
                conversationId,
                currentUserId
        );
        List<Mensaje> messages = new ArrayList<>(messageRepository
                .findByIdConversacionOrderByFechaEnvioDesc(
                        conversation.getIdConversacion(),
                        PaginationSupport.page(page, size)
                )
                .getContent());
        Collections.reverse(messages);
        Map<Integer, Usuario> users = mapById(
                userRepository.findAllById(
                        ids(messages, Mensaje::getIdUsuarioEmisor)
                ),
                Usuario::getIdUsuario
        );
        return messages.stream()
                .map(item -> toMessageResponse(
                        item,
                        currentUserId,
                        required(users, item.getIdUsuarioEmisor(), "usuario")
                ))
                .toList();
    }

    @Transactional
    public MessageResponse sendMessage(
            Integer currentUserId,
            Integer conversationId,
            CreateMessageRequest request
    ) {
        Conversacion conversation = requireParticipant(
                conversationId,
                currentUserId
        );
        if (!CONVERSATION_OPEN.equals(conversation.getEstadoConversacion())) {
            throw new ResourceConflictException(
                    "La conversacion ya no admite nuevos mensajes."
            );
        }

        LocalDateTime now = LocalDateTime.now();
        Mensaje message = new Mensaje();
        message.setIdConversacion(conversationId);
        message.setIdUsuarioEmisor(currentUserId);
        message.setContenido(request.contenido().trim());
        message.setFechaEnvio(now);
        message.setLeido(false);
        message.setFechaLectura(null);
        Mensaje saved = messageRepository.save(message);

        conversation.setFechaUltimoMensaje(now);
        conversationRepository.save(conversation);

        Integer recipientId = resolveCounterpartId(conversation, currentUserId);
        Tarea task = requireTask(conversation.getIdTarea());
        Usuario sender = requireUser(currentUserId);
        notificationService.create(
                recipientId,
                "Nuevo mensaje de " + fullName(sender),
                task.getTitulo() + ": " + preview(saved.getContenido())
        );
        return toMessageResponse(saved, currentUserId);
    }

    @Transactional
    public void markRead(Integer currentUserId, Integer conversationId) {
        requireParticipant(conversationId, currentUserId);
        messageRepository.markConversationAsRead(
                conversationId,
                currentUserId,
                LocalDateTime.now()
        );
    }

    private Conversacion ensureForJob(
            TrabajoAsignado job,
            Integer applicationId
    ) {
        return conversationRepository.findByIdTrabajo(job.getIdTrabajo())
                .orElseGet(() -> {
                    Conversacion conversation = new Conversacion();
                    conversation.setIdTarea(job.getIdTarea());
                    conversation.setIdPostulacion(applicationId);
                    conversation.setIdTrabajo(job.getIdTrabajo());
                    conversation.setEstadoConversacion(CONVERSATION_OPEN);
                    conversation.setFechaCreacion(LocalDateTime.now());
                    conversation.setFechaUltimoMensaje(null);
                    return conversationRepository.save(conversation);
                });
    }

    private ConversationResponse toResponse(
            Conversacion conversation,
            Integer currentUserId,
            Map<Integer, Tarea> tasks,
            Map<Integer, TrabajoAsignado> jobs,
            Map<Integer, Postulacion> applications,
            Map<Integer, Usuario> users,
            Map<Integer, Mensaje> latestMessages,
            Map<Integer, Long> unreadCounts
    ) {
        Tarea task = required(tasks, conversation.getIdTarea(), "oportunidad");
        Integer studentId = resolveStudentId(conversation, jobs, applications);
        Integer counterpartId = task.getIdCliente().equals(currentUserId)
                ? studentId
                : task.getIdCliente();
        Usuario counterpart = required(users, counterpartId, "usuario");
        Mensaje lastMessage = latestMessages.get(conversation.getIdConversacion());
        return new ConversationResponse(
                conversation.getIdConversacion(),
                conversation.getIdTarea(),
                conversation.getIdTrabajo(),
                task.getTitulo(),
                counterpartId,
                fullName(counterpart),
                counterpart.getNombreUsuario(),
                conversation.getEstadoConversacion(),
                lastMessage == null ? null : lastMessage.getContenido(),
                lastMessage == null
                        ? conversation.getFechaCreacion()
                        : lastMessage.getFechaEnvio(),
                unreadCounts.getOrDefault(conversation.getIdConversacion(), 0L)
        );
    }

    private MessageResponse toMessageResponse(
            Mensaje message,
            Integer currentUserId
    ) {
        return toMessageResponse(
                message,
                currentUserId,
                requireUser(message.getIdUsuarioEmisor())
        );
    }

    private MessageResponse toMessageResponse(
            Mensaje message,
            Integer currentUserId,
            Usuario sender
    ) {
        return new MessageResponse(
                message.getIdMensaje(),
                message.getIdConversacion(),
                message.getIdUsuarioEmisor(),
                fullName(sender),
                sender.getNombreUsuario(),
                message.getContenido(),
                message.getFechaEnvio(),
                message.isLeido(),
                message.getFechaLectura(),
                message.getIdUsuarioEmisor().equals(currentUserId)
        );
    }

    private Conversacion requireParticipant(
            Integer conversationId,
            Integer currentUserId
    ) {
        Conversacion conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "La conversacion indicada no existe."
                ));
        Integer counterpartId = resolveCounterpartId(
                conversation,
                currentUserId
        );
        if (counterpartId == null) {
            throw new ForbiddenOperationException(
                    "Solo los participantes pueden acceder a esta conversacion."
            );
        }
        return conversation;
    }

    private Integer resolveCounterpartId(
            Conversacion conversation,
            Integer currentUserId
    ) {
        Tarea task = requireTask(conversation.getIdTarea());
        Integer studentId = resolveStudentId(conversation);
        if (task.getIdCliente().equals(currentUserId)) {
            return studentId;
        }
        if (studentId.equals(currentUserId)) {
            return task.getIdCliente();
        }
        return null;
    }

    private Integer resolveStudentId(Conversacion conversation) {
        if (conversation.getIdTrabajo() != null) {
            return jobRepository.findById(conversation.getIdTrabajo())
                    .map(TrabajoAsignado::getIdEstudiante)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "El trabajo de la conversacion no existe."
                    ));
        }
        if (conversation.getIdPostulacion() != null) {
            return applicationRepository.findById(
                            conversation.getIdPostulacion()
                    )
                    .map(Postulacion::getIdEstudiante)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "La postulacion de la conversacion no existe."
                    ));
        }
        throw new ResourceConflictException(
                "La conversacion no tiene participantes completos."
        );
    }

    private Integer resolveStudentId(
            Conversacion conversation,
            Map<Integer, TrabajoAsignado> jobs,
            Map<Integer, Postulacion> applications
    ) {
        if (conversation.getIdTrabajo() != null) {
            return required(jobs, conversation.getIdTrabajo(), "trabajo")
                    .getIdEstudiante();
        }
        if (conversation.getIdPostulacion() != null) {
            return required(
                    applications,
                    conversation.getIdPostulacion(),
                    "postulacion"
            ).getIdEstudiante();
        }
        throw new ResourceConflictException(
                "La conversacion no tiene participantes completos."
        );
    }

    private <T, K> Map<K, T> mapById(
            Iterable<T> items,
            Function<T, K> idExtractor
    ) {
        Map<K, T> result = new java.util.HashMap<>();
        items.forEach(item -> result.put(idExtractor.apply(item), item));
        return result;
    }

    private <T> Set<Integer> ids(
            List<T> items,
            Function<T, Integer> idExtractor
    ) {
        return items.stream()
                .map(idExtractor)
                .collect(Collectors.toSet());
    }

    private <T> Set<Integer> nullableIds(
            List<T> items,
            Function<T, Integer> idExtractor
    ) {
        return items.stream()
                .map(idExtractor)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
    }

    private <K, T> T required(Map<K, T> items, K id, String resource) {
        T item = items.get(id);
        if (item == null) {
            throw new ResourceNotFoundException(
                    "No se encontro el recurso asociado: " + resource + "."
            );
        }
        return item;
    }

    private Tarea requireTask(Integer taskId) {
        return taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "La oportunidad de la conversacion no existe."
                ));
    }

    private Usuario requireUser(Integer userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "El usuario de la conversacion no existe."
                ));
    }

    private String fullName(Usuario user) {
        return (user.getNombre() + " " + user.getApellido()).trim();
    }

    private String preview(String content) {
        return content.length() <= 140
                ? content
                : content.substring(0, 140) + "...";
    }
}
