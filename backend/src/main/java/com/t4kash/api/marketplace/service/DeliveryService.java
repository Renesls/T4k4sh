package com.t4kash.api.marketplace.service;

import com.t4kash.api.communication.service.NotificationService;
import com.t4kash.api.exception.ResourceConflictException;
import com.t4kash.api.exception.ResourceNotFoundException;
import com.t4kash.api.finance.service.PaymentService;
import com.t4kash.api.marketplace.dto.CreateDeliveryRequest;
import com.t4kash.api.marketplace.dto.DeliveryResponse;
import com.t4kash.api.marketplace.entity.Entrega;
import com.t4kash.api.marketplace.entity.Tarea;
import com.t4kash.api.marketplace.entity.TrabajoAsignado;
import com.t4kash.api.marketplace.repository.EntregaRepository;
import com.t4kash.api.marketplace.repository.TrabajoAsignadoRepository;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/** Gestiona el envio y la aprobacion de entregas asociadas a un trabajo. */
@Service
public class DeliveryService {
    private static final String ESTADO_TRABAJO_EN_PROCESO = "EN_PROCESO";
    private static final String ESTADO_TRABAJO_FINALIZADO = "FINALIZADO";
    private static final String ESTADO_ENTREGA_ENVIADA = "ENVIADA";
    private static final String ESTADO_ENTREGA_APROBADA = "APROBADA";

    private final EntregaRepository entregaRepository;
    private final TrabajoAsignadoRepository trabajoRepository;
    private final TaskService taskService;
    private final JobService jobService;
    private final NotificationService notificationService;
    private final PaymentService paymentService;

    @Autowired
    public DeliveryService(
            EntregaRepository entregaRepository,
            TrabajoAsignadoRepository trabajoRepository,
            TaskService taskService,
            JobService jobService,
            NotificationService notificationService,
            PaymentService paymentService
    ) {
        this.entregaRepository = entregaRepository;
        this.trabajoRepository = trabajoRepository;
        this.taskService = taskService;
        this.jobService = jobService;
        this.notificationService = notificationService;
        this.paymentService = paymentService;
    }

    public DeliveryService(
            EntregaRepository entregaRepository,
            TrabajoAsignadoRepository trabajoRepository,
            TaskService taskService,
            JobService jobService,
            NotificationService notificationService
    ) {
        this(
                entregaRepository, trabajoRepository, taskService,
                jobService, notificationService, null
        );
    }

    @Transactional
    public DeliveryResponse createDelivery(
            Integer currentUserId,
            Integer idTrabajo,
            CreateDeliveryRequest request
    ) {
        TrabajoAsignado trabajo = jobService.findJobEntity(idTrabajo);
        jobService.requireAssignedStudent(trabajo, currentUserId);
        if (!ESTADO_TRABAJO_EN_PROCESO.equals(trabajo.getEstadoTrabajo())) {
            throw new ResourceConflictException("Solo se pueden enviar entregas para trabajos en proceso.");
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
        return DeliveryResponse.fromEntity(savedDelivery);
    }

    @Transactional(readOnly = true)
    public List<DeliveryResponse> listDeliveries(Integer currentUserId, Integer idTrabajo) {
        TrabajoAsignado trabajo = jobService.findJobEntity(idTrabajo);
        jobService.requireJobParticipant(trabajo, currentUserId);
        return entregaRepository.findByIdTrabajoOrderByFechaEntregaDesc(idTrabajo)
                .stream()
                .map(DeliveryResponse::fromEntity)
                .toList();
    }

    @Transactional
    public DeliveryResponse approveDelivery(Integer currentUserId, Integer idEntrega) {
        Entrega entrega = findDelivery(idEntrega);
        if (!ESTADO_ENTREGA_ENVIADA.equals(entrega.getEstadoEntrega())) {
            throw new ResourceConflictException("Solo se pueden aprobar entregas enviadas.");
        }

        TrabajoAsignado trabajo = jobService.findJobEntity(entrega.getIdTrabajo());
        Tarea task = taskService.findTaskEntity(trabajo.getIdTarea());
        taskService.requireTaskOwner(task, currentUserId);
        boolean finished = paymentService == null
                || paymentService.releaseForApprovedDelivery(trabajo);
        entrega.setEstadoEntrega(ESTADO_ENTREGA_APROBADA);
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
        return DeliveryResponse.fromEntity(savedDelivery);
    }

    private Entrega findDelivery(Integer idEntrega) {
        return entregaRepository.findById(idEntrega)
                .orElseThrow(() -> new ResourceNotFoundException("La entrega indicada no existe."));
    }
}
