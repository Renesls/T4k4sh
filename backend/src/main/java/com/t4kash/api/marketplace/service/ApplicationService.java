package com.t4kash.api.marketplace.service;

import com.t4kash.api.communication.service.ConversationService;
import com.t4kash.api.communication.service.NotificationService;
import com.t4kash.api.exception.ForbiddenOperationException;
import com.t4kash.api.exception.ResourceConflictException;
import com.t4kash.api.exception.ResourceNotFoundException;
import com.t4kash.api.marketplace.dto.ApplicationResponse;
import com.t4kash.api.marketplace.dto.CreateApplicationRequest;
import com.t4kash.api.marketplace.dto.JobResponse;
import com.t4kash.api.marketplace.entity.Postulacion;
import com.t4kash.api.marketplace.entity.Tarea;
import com.t4kash.api.marketplace.entity.TrabajoAsignado;
import com.t4kash.api.marketplace.repository.PostulacionRepository;
import com.t4kash.api.marketplace.repository.TrabajoAsignadoRepository;
import com.t4kash.api.marketplace.repository.UsuarioEstudianteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Gestiona el ciclo de las postulaciones, desde su creacion hasta su
 * aceptacion o rechazo y la asignacion resultante. Delega en TaskService las
 * consultas y validaciones de propiedad para no duplicar reglas.
 */
@Service
public class ApplicationService {
    private static final String ESTADO_TAREA_PUBLICADA = "PUBLICADA";
    private static final String ESTADO_TAREA_ASIGNADA = "ASIGNADA";
    private static final String ESTADO_POSTULACION_PENDIENTE = "PENDIENTE";
    private static final String ESTADO_POSTULACION_ACEPTADA = "ACEPTADA";
    private static final String ESTADO_POSTULACION_RECHAZADA = "RECHAZADA";
    private static final String ESTADO_POSTULACION_CANCELADA_LIMITE = "CANCELADA_LIMITE";
    private static final String ESTADO_POSTULACION_CANCELADA_TAREA = "CANCELADA_TAREA";
    private static final String ESTADO_TRABAJO_EN_PROCESO = "EN_PROCESO";
    private static final int MAX_ACTIVE_JOBS = 2;
    private static final int MAX_APPLICATION_ATTEMPTS = 3;

    private final PostulacionRepository postulacionRepository;
    private final TrabajoAsignadoRepository trabajoRepository;
    private final UsuarioEstudianteRepository estudianteRepository;
    private final TaskService taskService;
    private final ConversationService conversationService;
    private final NotificationService notificationService;

    public ApplicationService(
            PostulacionRepository postulacionRepository,
            TrabajoAsignadoRepository trabajoRepository,
            UsuarioEstudianteRepository estudianteRepository,
            TaskService taskService,
            ConversationService conversationService,
            NotificationService notificationService
    ) {
        this.postulacionRepository = postulacionRepository;
        this.trabajoRepository = trabajoRepository;
        this.estudianteRepository = estudianteRepository;
        this.taskService = taskService;
        this.conversationService = conversationService;
        this.notificationService = notificationService;
    }

    @Transactional(readOnly = true)
    public List<ApplicationResponse> listApplications(Integer currentUserId, Integer idTarea) {
        Tarea tarea = taskService.findTaskEntity(idTarea);
        taskService.requireTaskOwner(tarea, currentUserId);
        return postulacionRepository.findByIdTareaOrderByFechaPostulacionDesc(idTarea)
                .stream()
                .map(ApplicationResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ApplicationResponse> listMyApplications(Integer currentUserId) {
        return postulacionRepository
                .findByIdEstudianteOrderByFechaPostulacionDesc(currentUserId)
                .stream()
                .map(ApplicationResponse::fromEntity)
                .toList();
    }

    @Transactional(noRollbackFor = ResourceConflictException.class)
    public ApplicationResponse applyToTask(
            Integer currentUserId,
            Integer idTarea,
            CreateApplicationRequest request
    ) {
        Tarea tarea = taskService.findTaskEntity(idTarea);
        if (taskService.closeExpiredTask(tarea, LocalDateTime.now())) {
            throw new ResourceConflictException(
                    "El plazo de postulacion para esta tarea ya finalizo."
            );
        }
        if (!ESTADO_TAREA_PUBLICADA.equals(tarea.getEstadoTarea())) {
            throw new ResourceConflictException("La tarea no esta disponible para nuevas postulaciones.");
        }
        if (tarea.getIdCliente().equals(currentUserId)) {
            throw new ResourceConflictException("No puedes postularte a tu propia tarea.");
        }
        if (!estudianteRepository.existsById(currentUserId)) {
            throw new ForbiddenOperationException(
                    "Tu cuenta no tiene un perfil estudiantil activo."
            );
        }
        if (activeJobs(currentUserId) >= MAX_ACTIVE_JOBS) {
            throw new ResourceConflictException(
                    "Ya tienes dos trabajos en proceso. Finaliza uno antes de postularte."
            );
        }
        Postulacion previous = postulacionRepository
                .findFirstByIdTareaAndIdEstudianteOrderByNumeroIntentoDesc(
                        idTarea,
                        currentUserId
                )
                .orElse(null);
        int attemptNumber = nextAttemptNumber(previous);

        Postulacion postulacion = new Postulacion();
        postulacion.setIdTarea(idTarea);
        postulacion.setIdEstudiante(currentUserId);
        postulacion.setMensaje(request.mensaje());
        postulacion.setPrecioPropuesto(request.precioPropuesto());
        postulacion.setFechaPostulacion(LocalDateTime.now());
        postulacion.setEstadoPostulacion(ESTADO_POSTULACION_PENDIENTE);
        postulacion.setNumeroIntento(attemptNumber);

        Postulacion savedApplication = postulacionRepository.save(postulacion);
        notificationService.create(
                tarea.getIdCliente(),
                "Nueva postulacion",
                "Recibiste una postulacion para " + tarea.getTitulo() + "."
        );
        return ApplicationResponse.fromEntity(savedApplication);
    }

    @Transactional
    public JobResponse acceptApplication(Integer currentUserId, Integer idPostulacion) {
        Postulacion postulacion = findApplication(idPostulacion);
        Tarea tarea = taskService.findTaskEntity(postulacion.getIdTarea());
        taskService.requireTaskOwner(tarea, currentUserId);
        if (!ESTADO_POSTULACION_PENDIENTE.equals(postulacion.getEstadoPostulacion())) {
            throw new ResourceConflictException("Solo se pueden aceptar postulaciones pendientes.");
        }
        if (trabajoRepository.findByIdTarea(postulacion.getIdTarea()).isPresent()) {
            throw new ResourceConflictException("Esta tarea ya tiene un trabajo asignado.");
        }
        estudianteRepository.findByIdForUpdate(postulacion.getIdEstudiante())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "El perfil estudiantil indicado no existe."
                ));
        long currentJobs = activeJobs(postulacion.getIdEstudiante());
        if (currentJobs >= MAX_ACTIVE_JOBS) {
            throw new ResourceConflictException(
                    "El estudiante ya alcanzo el limite de dos trabajos en proceso."
            );
        }

        postulacion.setEstadoPostulacion(ESTADO_POSTULACION_ACEPTADA);
        tarea.setEstadoTarea(ESTADO_TAREA_ASIGNADA);

        TrabajoAsignado trabajo = new TrabajoAsignado();
        trabajo.setIdTarea(postulacion.getIdTarea());
        trabajo.setIdEstudiante(postulacion.getIdEstudiante());
        trabajo.setFechaInicio(LocalDateTime.now());
        trabajo.setFechaEntregaEsperada(tarea.getFechaLimite());
        trabajo.setEstadoTrabajo(ESTADO_TRABAJO_EN_PROCESO);

        taskService.save(tarea);
        postulacionRepository.save(postulacion);
        rejectRemainingApplications(postulacion);
        TrabajoAsignado savedJob = trabajoRepository.save(trabajo);
        if (currentJobs + 1 >= MAX_ACTIVE_JOBS) {
            cancelPendingApplicationsForLimit(
                    postulacion.getIdEstudiante(),
                    postulacion.getIdPostulacion()
            );
        }
        conversationService.ensureForAcceptedApplication(
                postulacion,
                savedJob
        );
        notificationService.create(
                postulacion.getIdEstudiante(),
                "Postulacion aceptada",
                "Fuiste seleccionado para " + tarea.getTitulo() + "."
        );
        return JobResponse.fromEntity(savedJob);
    }

    @Transactional
    public ApplicationResponse rejectApplication(Integer currentUserId, Integer idPostulacion) {
        Postulacion postulacion = findApplication(idPostulacion);
        Tarea tarea = taskService.findTaskEntity(postulacion.getIdTarea());
        taskService.requireTaskOwner(tarea, currentUserId);
        if (!ESTADO_POSTULACION_PENDIENTE.equals(postulacion.getEstadoPostulacion())) {
            throw new ResourceConflictException("Solo se pueden rechazar postulaciones pendientes.");
        }
        postulacion.setEstadoPostulacion(ESTADO_POSTULACION_RECHAZADA);
        Postulacion savedApplication = postulacionRepository.save(postulacion);
        notificationService.create(
                postulacion.getIdEstudiante(),
                "Postulacion no seleccionada",
                "Tu postulacion para " + tarea.getTitulo() + " fue rechazada."
        );
        return ApplicationResponse.fromEntity(savedApplication);
    }

    private Postulacion findApplication(Integer idPostulacion) {
        return postulacionRepository.findById(idPostulacion)
                .orElseThrow(() -> new ResourceNotFoundException("La postulacion indicada no existe."));
    }

    private void rejectRemainingApplications(Postulacion acceptedApplication) {
        List<Postulacion> remainingApplications =
                postulacionRepository
                        .findByIdTareaAndEstadoPostulacionAndIdPostulacionNot(
                                acceptedApplication.getIdTarea(),
                                ESTADO_POSTULACION_PENDIENTE,
                                acceptedApplication.getIdPostulacion()
                        );
        remainingApplications.forEach(application ->
                application.setEstadoPostulacion(ESTADO_POSTULACION_RECHAZADA)
        );
        postulacionRepository.saveAll(remainingApplications);
    }

    private void cancelPendingApplicationsForLimit(
            Integer studentId,
            Integer acceptedApplicationId
    ) {
        List<Postulacion> pending = postulacionRepository
                .findByIdEstudianteAndEstadoPostulacion(
                        studentId,
                        ESTADO_POSTULACION_PENDIENTE
                );
        pending.stream()
                .filter(item -> !item.getIdPostulacion().equals(acceptedApplicationId))
                .forEach(item ->
                        item.setEstadoPostulacion(ESTADO_POSTULACION_CANCELADA_LIMITE)
                );
        postulacionRepository.saveAll(pending);
    }

    private int nextAttemptNumber(Postulacion previous) {
        if (previous == null) {
            return 1;
        }
        if (ESTADO_POSTULACION_PENDIENTE.equals(previous.getEstadoPostulacion())) {
            throw new ResourceConflictException(
                    "Ya tienes una postulacion pendiente para esta tarea."
            );
        }
        if (ESTADO_POSTULACION_ACEPTADA.equals(previous.getEstadoPostulacion())) {
            throw new ResourceConflictException(
                    "Tu postulacion para esta tarea ya fue aceptada."
            );
        }
        if (ESTADO_POSTULACION_CANCELADA_TAREA.equals(
                previous.getEstadoPostulacion()
        )) {
            throw new ResourceConflictException("La tarea fue cancelada.");
        }
        int nextAttempt = previous.getNumeroIntento() + 1;
        if (nextAttempt > MAX_APPLICATION_ATTEMPTS) {
            throw new ResourceConflictException(
                    "Ya utilizaste los tres intentos permitidos para esta tarea."
            );
        }
        return nextAttempt;
    }

    private long activeJobs(Integer studentId) {
        return trabajoRepository.countByIdEstudianteAndEstadoTrabajo(
                studentId,
                ESTADO_TRABAJO_EN_PROCESO
        );
    }
}
