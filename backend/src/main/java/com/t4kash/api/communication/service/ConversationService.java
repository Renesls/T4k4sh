package com.t4kash.api.communication.service;

import com.t4kash.api.communication.dto.ConversationResponse;
import com.t4kash.api.communication.dto.CreateMessageRequest;
import com.t4kash.api.communication.dto.MessageResponse;
import com.t4kash.api.communication.entity.Conversacion;
import com.t4kash.api.communication.entity.Mensaje;
import com.t4kash.api.communication.repository.ConversacionRepository;
import com.t4kash.api.communication.repository.MensajeRepository;
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
import java.util.List;

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

    @Transactional
    public List<ConversationResponse> listMine(Integer currentUserId) {
        jobRepository.findVisibleToUser(currentUserId)
                .forEach(job -> ensureForJob(job, null));
        return conversationRepository.findVisibleToUser(currentUserId)
                .stream()
                .map(item -> toResponse(item, currentUserId))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MessageResponse> listMessages(
            Integer currentUserId,
            Integer conversationId
    ) {
        Conversacion conversation = requireParticipant(
                conversationId,
                currentUserId
        );
        return messageRepository
                .findByIdConversacionOrderByFechaEnvioAsc(
                        conversation.getIdConversacion()
                )
                .stream()
                .map(item -> toMessageResponse(item, currentUserId))
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
            Integer currentUserId
    ) {
        Tarea task = requireTask(conversation.getIdTarea());
        Integer counterpartId = resolveCounterpartId(
                conversation,
                currentUserId
        );
        Usuario counterpart = requireUser(counterpartId);
        Mensaje lastMessage = messageRepository
                .findFirstByIdConversacionOrderByFechaEnvioDesc(
                        conversation.getIdConversacion()
                )
                .orElse(null);
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
                messageRepository
                        .countByIdConversacionAndIdUsuarioEmisorNotAndLeidoFalse(
                                conversation.getIdConversacion(),
                                currentUserId
                        )
        );
    }

    private MessageResponse toMessageResponse(
            Mensaje message,
            Integer currentUserId
    ) {
        Usuario sender = requireUser(message.getIdUsuarioEmisor());
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
