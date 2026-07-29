package com.t4kash.api.communication.service;

import com.t4kash.api.communication.dto.CreateMessageRequest;
import com.t4kash.api.communication.dto.MessageResponse;
import com.t4kash.api.communication.entity.Conversacion;
import com.t4kash.api.communication.entity.Mensaje;
import com.t4kash.api.communication.repository.ConversacionRepository;
import com.t4kash.api.communication.repository.MensajeRepository;
import com.t4kash.api.exception.ForbiddenOperationException;
import com.t4kash.api.marketplace.entity.Postulacion;
import com.t4kash.api.marketplace.entity.Tarea;
import com.t4kash.api.marketplace.entity.TrabajoAsignado;
import com.t4kash.api.marketplace.entity.Usuario;
import com.t4kash.api.marketplace.repository.PostulacionRepository;
import com.t4kash.api.marketplace.repository.TareaRepository;
import com.t4kash.api.marketplace.repository.TrabajoAsignadoRepository;
import com.t4kash.api.marketplace.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConversationServiceTest {
    @Mock
    private ConversacionRepository conversationRepository;
    @Mock
    private MensajeRepository messageRepository;
    @Mock
    private TareaRepository taskRepository;
    @Mock
    private PostulacionRepository applicationRepository;
    @Mock
    private TrabajoAsignadoRepository jobRepository;
    @Mock
    private UsuarioRepository userRepository;
    @Mock
    private NotificationService notificationService;

    private ConversationService service;

    @BeforeEach
    void setUp() {
        service = new ConversationService(
                conversationRepository,
                messageRepository,
                taskRepository,
                applicationRepository,
                jobRepository,
                userRepository,
                notificationService
        );
    }

    @Test
    void createsConversationForAcceptedApplication() {
        Postulacion application = application(21, 2);
        TrabajoAsignado job = job(30, 10, 2);
        when(conversationRepository.findByIdTrabajo(30))
                .thenReturn(Optional.empty());
        when(conversationRepository.save(any(Conversacion.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Conversacion result = service.ensureForAcceptedApplication(
                application,
                job
        );

        assertEquals(10, result.getIdTarea());
        assertEquals(21, result.getIdPostulacion());
        assertEquals(30, result.getIdTrabajo());
        assertEquals("ABIERTA", result.getEstadoConversacion());
    }

    @Test
    void sendsMessageAndNotifiesCounterpart() {
        Conversacion conversation = conversation(40, 10, 30);
        Tarea task = task(10, 1);
        TrabajoAsignado job = job(30, 10, 2);
        Usuario sender = user(1, "Ana", "Cliente");
        when(conversationRepository.findById(40))
                .thenReturn(Optional.of(conversation));
        when(taskRepository.findById(10)).thenReturn(Optional.of(task));
        when(jobRepository.findById(30)).thenReturn(Optional.of(job));
        when(userRepository.findById(1)).thenReturn(Optional.of(sender));
        when(messageRepository.save(any(Mensaje.class)))
                .thenAnswer(invocation -> {
                    Mensaje saved = invocation.getArgument(0);
                    saved.setIdMensaje(50);
                    return saved;
                });

        MessageResponse response = service.sendMessage(
                1,
                40,
                new CreateMessageRequest("Ya puedes iniciar.")
        );

        assertEquals(50, response.idMensaje());
        assertEquals("Ya puedes iniciar.", response.contenido());
        verify(notificationService).create(
                2,
                "Nuevo mensaje de Ana Cliente",
                "Diseno de afiche: Ya puedes iniciar."
        );
    }

    @Test
    void rejectsUserOutsideConversation() {
        when(conversationRepository.findById(40))
                .thenReturn(Optional.of(conversation(40, 10, 30)));
        when(taskRepository.findById(10))
                .thenReturn(Optional.of(task(10, 1)));
        when(jobRepository.findById(30))
                .thenReturn(Optional.of(job(30, 10, 2)));

        assertThrows(
                ForbiddenOperationException.class,
                () -> service.listMessages(99, 40)
        );
    }

    @Test
    void marksOnlyCounterpartMessagesAsRead() {
        when(conversationRepository.findById(40))
                .thenReturn(Optional.of(conversation(40, 10, 30)));
        when(taskRepository.findById(10))
                .thenReturn(Optional.of(task(10, 1)));
        when(jobRepository.findById(30))
                .thenReturn(Optional.of(job(30, 10, 2)));

        service.markRead(2, 40);

        verify(messageRepository).markConversationAsRead(
                any(),
                any(),
                any(LocalDateTime.class)
        );
    }

    private Conversacion conversation(
            Integer id,
            Integer taskId,
            Integer jobId
    ) {
        Conversacion conversation = new Conversacion();
        conversation.setIdConversacion(id);
        conversation.setIdTarea(taskId);
        conversation.setIdTrabajo(jobId);
        conversation.setEstadoConversacion("ABIERTA");
        conversation.setFechaCreacion(LocalDateTime.now());
        return conversation;
    }

    private Postulacion application(Integer id, Integer studentId) {
        Postulacion application = new Postulacion();
        application.setIdPostulacion(id);
        application.setIdEstudiante(studentId);
        application.setIdTarea(10);
        return application;
    }

    private TrabajoAsignado job(
            Integer id,
            Integer taskId,
            Integer studentId
    ) {
        TrabajoAsignado job = new TrabajoAsignado();
        job.setIdTrabajo(id);
        job.setIdTarea(taskId);
        job.setIdEstudiante(studentId);
        return job;
    }

    private Tarea task(Integer id, Integer ownerId) {
        Tarea task = new Tarea();
        task.setIdTarea(id);
        task.setIdCliente(ownerId);
        task.setTitulo("Diseno de afiche");
        return task;
    }

    private Usuario user(
            Integer id,
            String firstName,
            String lastName
    ) {
        Usuario user = new Usuario();
        user.setIdUsuario(id);
        user.setNombre(firstName);
        user.setApellido(lastName);
        return user;
    }
}
