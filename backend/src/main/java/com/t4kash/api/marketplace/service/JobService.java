package com.t4kash.api.marketplace.service;

import com.t4kash.api.exception.ForbiddenOperationException;
import com.t4kash.api.exception.ResourceNotFoundException;
import com.t4kash.api.finance.dto.PaymentResponse;
import com.t4kash.api.finance.entity.Pago;
import com.t4kash.api.finance.repository.PagoRepository;
import com.t4kash.api.marketplace.dto.JobResponse;
import com.t4kash.api.marketplace.entity.Tarea;
import com.t4kash.api.marketplace.entity.TrabajoAsignado;
import com.t4kash.api.marketplace.repository.TrabajoAsignadoRepository;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Centraliza las consultas de TrabajoAsignado que utilizan DeliveryService y
 * CalificacionService. La creacion permanece en ApplicationService.acceptApplication
 * porque forma parte de aceptar una postulacion y no constituye una operacion aislada.
 */
@Service
public class JobService {
    private final TrabajoAsignadoRepository trabajoRepository;
    private final TaskService taskService;
    private final PagoRepository paymentRepository;

    @Autowired
    public JobService(
            TrabajoAsignadoRepository trabajoRepository,
            TaskService taskService,
            PagoRepository paymentRepository
    ) {
        this.trabajoRepository = trabajoRepository;
        this.taskService = taskService;
        this.paymentRepository = paymentRepository;
    }

    public JobService(TrabajoAsignadoRepository trabajoRepository, TaskService taskService) {
        this(trabajoRepository, taskService, null);
    }

    @Transactional(readOnly = true)
    public List<JobResponse> listJobs(Integer currentUserId) {
        return trabajoRepository.findVisibleToUser(currentUserId)
                .stream()
                .map(job -> {
                    JobResponse response = JobResponse.fromEntity(job);
                    Pago payment = paymentRepository == null
                            ? null
                            : paymentRepository.findByIdTrabajo(job.getIdTrabajo()).orElse(null);
                    return payment == null
                            ? response
                            : response.withPayment(PaymentResponse.fromEntity(payment, currentUserId));
                })
                .toList();
    }

    public TrabajoAsignado findJobEntity(Integer idTrabajo) {
        return trabajoRepository.findById(idTrabajo)
                .orElseThrow(() -> new ResourceNotFoundException("El trabajo indicado no existe."));
    }

    public TrabajoAsignado findJobEntityForUpdate(Integer idTrabajo) {
        return trabajoRepository.findByIdForUpdate(idTrabajo)
                .orElseThrow(() -> new ResourceNotFoundException("El trabajo indicado no existe."));
    }

    public void requireAssignedStudent(TrabajoAsignado trabajo, Integer currentUserId) {
        if (!trabajo.getIdEstudiante().equals(currentUserId)) {
            throw new ForbiddenOperationException(
                    "Solo el estudiante asignado puede realizar esta accion."
            );
        }
    }

    public void requireJobParticipant(TrabajoAsignado trabajo, Integer currentUserId) {
        Tarea tarea = taskService.findTaskEntity(trabajo.getIdTarea());
        boolean isParticipant =
                trabajo.getIdEstudiante().equals(currentUserId) ||
                tarea.getIdCliente().equals(currentUserId);
        if (!isParticipant) {
            throw new ForbiddenOperationException(
                    "Solo los participantes del trabajo pueden consultar esta informacion."
            );
        }
    }
}
