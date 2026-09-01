package com.t4kash.api.marketplace.service;

import com.t4kash.api.communication.service.NotificationService;
import com.t4kash.api.exception.ResourceConflictException;
import com.t4kash.api.exception.ResourceNotFoundException;
import com.t4kash.api.finance.service.PaymentService;
import com.t4kash.api.marketplace.dto.CreateDeliveryCommentRequest;
import com.t4kash.api.marketplace.dto.CreateDeliveryRequest;
import com.t4kash.api.marketplace.dto.DeliveryCommentResponse;
import com.t4kash.api.marketplace.dto.DeliveryResponse;
import com.t4kash.api.marketplace.dto.DeliveryReviewResponse;
import com.t4kash.api.marketplace.dto.RequestDeliveryChangesRequest;
import com.t4kash.api.marketplace.entity.ComentarioEntrega;
import com.t4kash.api.marketplace.entity.Entrega;
import com.t4kash.api.marketplace.entity.RevisionEntrega;
import com.t4kash.api.marketplace.entity.Tarea;
import com.t4kash.api.marketplace.entity.TrabajoAsignado;
import com.t4kash.api.marketplace.repository.ComentarioEntregaRepository;
import com.t4kash.api.marketplace.repository.EntregaRepository;
import com.t4kash.api.marketplace.repository.RevisionEntregaRepository;
import com.t4kash.api.marketplace.repository.TrabajoAsignadoRepository;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Gestiona el envio y la aprobacion de entregas asociadas a un trabajo. */
@Service
public class DeliveryService {
    private static final String ESTADO_TRABAJO_EN_PROCESO = "EN_PROCESO";
    private static final String ESTADO_TRABAJO_FINALIZADO = "FINALIZADO";
    private static final String ESTADO_ENTREGA_ENVIADA = "ENVIADA";
    private static final String ESTADO_ENTREGA_APROBADA = "APROBADA";
    private static final String ESTADO_ENTREGA_CAMBIOS = "CAMBIOS_SOLICITADOS";
    private static final String ESTADO_REVISION_REGISTRADA = "REGISTRADA";

    private final EntregaRepository entregaRepository;
    private final ComentarioEntregaRepository comentarioRepository;
    private final RevisionEntregaRepository revisionRepository;
    private final TrabajoAsignadoRepository trabajoRepository;
    private final TaskService taskService;
    private final JobService jobService;
    private final NotificationService notificationService;
    private final PaymentService paymentService;

    @Autowired
    public DeliveryService(
            EntregaRepository entregaRepository,
            ComentarioEntregaRepository comentarioRepository,
            RevisionEntregaRepository revisionRepository,
            TrabajoAsignadoRepository trabajoRepository,
            TaskService taskService,
            JobService jobService,
            NotificationService notificationService,
            PaymentService paymentService
    ) {
        this.entregaRepository = entregaRepository;
        this.comentarioRepository = comentarioRepository;
        this.revisionRepository = revisionRepository;
        this.trabajoRepository = trabajoRepository;
        this.taskService = taskService;
        this.jobService = jobService;
        this.notificationService = notificationService;
        this.paymentService = paymentService;
    }

    @Transactional
    public DeliveryResponse createDelivery(
            Integer currentUserId,
            Integer idTrabajo,
            CreateDeliveryRequest request
    ) {
        TrabajoAsignado trabajo = jobService.findJobEntityForUpdate(idTrabajo);
        jobService.requireAssignedStudent(trabajo, currentUserId);
        if (!ESTADO_TRABAJO_EN_PROCESO.equals(trabajo.getEstadoTrabajo())) {
            throw new ResourceConflictException("Solo se pueden enviar entregas para trabajos en proceso.");
        }
        if (entregaRepository.existsByIdTrabajoAndEstadoEntrega(
                idTrabajo,
                ESTADO_ENTREGA_ENVIADA
        )) {
            throw new ResourceConflictException(
                    "Ya existe una entrega pendiente de revision. Espera la respuesta del cliente."
            );
        }

        Entrega entrega = new Entrega();
        entrega.setIdTrabajo(idTrabajo);
        entrega.setDescripcionEntrega(request.descripcionEntrega().trim());
        entrega.setFechaEntrega(LocalDateTime.now());
        entrega.setEstadoEntrega(ESTADO_ENTREGA_ENVIADA);

        Entrega savedDelivery = entregaRepository.save(entrega);
        Tarea task = taskService.findTaskEntity(trabajo.getIdTarea());
        notificationService.create(
                task.getIdCliente(),
                "Nueva entrega",
                "Recibiste una entrega para " + task.getTitulo() + "."
        );
        return deliveryResponse(savedDelivery);
    }

    @Transactional(readOnly = true)
    public List<DeliveryResponse> listDeliveries(Integer currentUserId, Integer idTrabajo) {
        TrabajoAsignado trabajo = jobService.findJobEntity(idTrabajo);
        jobService.requireJobParticipant(trabajo, currentUserId);
        List<Entrega> deliveries = entregaRepository
                .findByIdTrabajoOrderByFechaEntregaDesc(idTrabajo);
        return deliveryResponses(deliveries);
    }

    @Transactional
    public DeliveryResponse approveDelivery(Integer currentUserId, Integer idEntrega) {
        Entrega entrega = findDeliveryForUpdate(idEntrega);
        if (!ESTADO_ENTREGA_ENVIADA.equals(entrega.getEstadoEntrega())) {
            throw new ResourceConflictException("Solo se pueden aprobar entregas enviadas.");
        }

        TrabajoAsignado trabajo = jobService.findJobEntityForUpdate(entrega.getIdTrabajo());
        if (!ESTADO_TRABAJO_EN_PROCESO.equals(trabajo.getEstadoTrabajo())) {
            throw new ResourceConflictException("Este trabajo ya no admite nuevas decisiones.");
        }
        Tarea task = taskService.findTaskEntity(trabajo.getIdTarea());
        taskService.requireTaskOwner(task, currentUserId);
        boolean finished = paymentService == null
                || paymentService.releaseForApprovedDelivery(trabajo);
        entrega.setEstadoEntrega(ESTADO_ENTREGA_APROBADA);
        revisionRepository.save(createReview(
                entrega,
                currentUserId,
                ESTADO_ENTREGA_APROBADA,
                null
        ));
        trabajo.setEstadoTrabajo(
                finished
                        ? ESTADO_TRABAJO_FINALIZADO
                        : PaymentService.JOB_CASH_CONFIRMATION_PENDING
        );

        trabajoRepository.save(trabajo);
        Entrega savedDelivery = entregaRepository.save(entrega);
        notificationService.create(
                trabajo.getIdEstudiante(),
                "Entrega aprobada",
                finished
                        ? "Tu entrega para " + task.getTitulo() + " fue aprobada."
                        : "Tu entrega fue aprobada. Confirma que recibiste el efectivo."
        );
        return deliveryResponse(savedDelivery);
    }

    @Transactional
    public DeliveryResponse requestChanges(
            Integer currentUserId,
            Integer idEntrega,
            RequestDeliveryChangesRequest request
    ) {
        Entrega entrega = findDeliveryForUpdate(idEntrega);
        if (!ESTADO_ENTREGA_ENVIADA.equals(entrega.getEstadoEntrega())) {
            throw new ResourceConflictException(
                    "Solo se pueden solicitar cambios sobre entregas enviadas."
            );
        }

        TrabajoAsignado trabajo = jobService.findJobEntityForUpdate(entrega.getIdTrabajo());
        if (!ESTADO_TRABAJO_EN_PROCESO.equals(trabajo.getEstadoTrabajo())) {
            throw new ResourceConflictException("Este trabajo ya no admite correcciones.");
        }
        Tarea task = taskService.findTaskEntity(trabajo.getIdTarea());
        taskService.requireTaskOwner(task, currentUserId);

        entrega.setEstadoEntrega(ESTADO_ENTREGA_CAMBIOS);
        revisionRepository.save(createReview(
                entrega,
                currentUserId,
                ESTADO_ENTREGA_CAMBIOS,
                request.observacion().trim()
        ));
        Entrega savedDelivery = entregaRepository.save(entrega);
        notificationService.create(
                trabajo.getIdEstudiante(),
                "Cambios solicitados",
                "El cliente solicito ajustes en tu entrega para " + task.getTitulo() + "."
        );
        return deliveryResponse(savedDelivery);
    }

    @Transactional
    public DeliveryResponse addComment(
            Integer currentUserId,
            Integer idEntrega,
            CreateDeliveryCommentRequest request
    ) {
        Entrega entrega = findDelivery(idEntrega);
        TrabajoAsignado trabajo = jobService.findJobEntity(entrega.getIdTrabajo());
        jobService.requireJobParticipant(trabajo, currentUserId);
        if (ESTADO_TRABAJO_FINALIZADO.equals(trabajo.getEstadoTrabajo())) {
            throw new ResourceConflictException(
                    "El historial de un trabajo finalizado ya no admite comentarios."
            );
        }
        Tarea task = taskService.findTaskEntity(trabajo.getIdTarea());
        boolean isStudent = trabajo.getIdEstudiante().equals(currentUserId);

        ComentarioEntrega comment = new ComentarioEntrega();
        comment.setIdEntrega(idEntrega);
        comment.setIdUsuario(currentUserId);
        comment.setComentario(request.comentario().trim());
        comment.setTipoComentario(isStudent ? "ESTUDIANTE" : "CLIENTE");
        comment.setFechaComentario(LocalDateTime.now());
        comentarioRepository.save(comment);

        notificationService.create(
                isStudent ? task.getIdCliente() : trabajo.getIdEstudiante(),
                "Nuevo comentario en una entrega",
                "Hay un nuevo comentario en el trabajo " + task.getTitulo() + "."
        );
        return deliveryResponse(entrega);
    }

    private Entrega findDelivery(Integer idEntrega) {
        return entregaRepository.findById(idEntrega)
                .orElseThrow(() -> new ResourceNotFoundException("La entrega indicada no existe."));
    }

    private Entrega findDeliveryForUpdate(Integer idEntrega) {
        return entregaRepository.findByIdForUpdate(idEntrega)
                .orElseThrow(() -> new ResourceNotFoundException("La entrega indicada no existe."));
    }

    private RevisionEntrega createReview(
            Entrega delivery,
            Integer reviewerId,
            String result,
            String observation
    ) {
        RevisionEntrega review = new RevisionEntrega();
        review.setIdEntrega(delivery.getIdEntrega());
        review.setIdUsuarioRevisa(reviewerId);
        review.setResultadoRevision(result);
        review.setObservacion(observation);
        review.setFechaRevision(LocalDateTime.now());
        review.setEstadoRevision(ESTADO_REVISION_REGISTRADA);
        return review;
    }

    private DeliveryResponse deliveryResponse(Entrega delivery) {
        List<DeliveryCommentResponse> comments = comentarioRepository
                .findByIdEntregaOrderByFechaComentarioAsc(delivery.getIdEntrega())
                .stream()
                .map(DeliveryCommentResponse::fromEntity)
                .toList();
        List<DeliveryReviewResponse> reviews = revisionRepository
                .findByIdEntregaOrderByFechaRevisionAsc(delivery.getIdEntrega())
                .stream()
                .map(DeliveryReviewResponse::fromEntity)
                .toList();
        return DeliveryResponse.fromEntity(delivery, comments, reviews);
    }

    private List<DeliveryResponse> deliveryResponses(List<Entrega> deliveries) {
        if (deliveries.isEmpty()) {
            return List.of();
        }
        List<Integer> deliveryIds = deliveries.stream().map(Entrega::getIdEntrega).toList();
        Map<Integer, List<DeliveryCommentResponse>> commentsByDelivery = new HashMap<>();
        comentarioRepository.findByIdEntregaInOrderByFechaComentarioAsc(deliveryIds)
                .forEach(comment -> commentsByDelivery
                        .computeIfAbsent(comment.getIdEntrega(), ignored -> new java.util.ArrayList<>())
                        .add(DeliveryCommentResponse.fromEntity(comment)));
        Map<Integer, List<DeliveryReviewResponse>> reviewsByDelivery = new HashMap<>();
        revisionRepository.findByIdEntregaInOrderByFechaRevisionAsc(deliveryIds)
                .forEach(review -> reviewsByDelivery
                        .computeIfAbsent(review.getIdEntrega(), ignored -> new java.util.ArrayList<>())
                        .add(DeliveryReviewResponse.fromEntity(review)));
        return deliveries.stream()
                .map(delivery -> DeliveryResponse.fromEntity(
                        delivery,
                        commentsByDelivery.getOrDefault(delivery.getIdEntrega(), List.of()),
                        reviewsByDelivery.getOrDefault(delivery.getIdEntrega(), List.of())
                ))
                .toList();
    }
}
