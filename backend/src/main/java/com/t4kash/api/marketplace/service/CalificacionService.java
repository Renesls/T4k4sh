package com.t4kash.api.marketplace.service;

import com.t4kash.api.exception.ForbiddenOperationException;
import com.t4kash.api.exception.ResourceConflictException;
import com.t4kash.api.marketplace.dto.CreateRatingRequest;
import com.t4kash.api.marketplace.dto.RatingResponse;
import com.t4kash.api.marketplace.entity.Calificacion;
import com.t4kash.api.marketplace.entity.Tarea;
import com.t4kash.api.marketplace.entity.TrabajoAsignado;
import com.t4kash.api.marketplace.repository.CalificacionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Evaluaciones mutuas entre cliente y estudiante al finalizar un trabajo.
 * El calificado se deriva de quien es el usuario actual dentro del trabajo,
 * asi que auto-calificarse es estructuralmente imposible, no solo validado.
 */
@Service
public class CalificacionService {
    private static final String ESTADO_TRABAJO_FINALIZADO = "FINALIZADO";

    private final CalificacionRepository calificacionRepository;
    private final JobService jobService;
    private final TaskService taskService;

    public CalificacionService(
            CalificacionRepository calificacionRepository,
            JobService jobService,
            TaskService taskService
    ) {
        this.calificacionRepository = calificacionRepository;
        this.jobService = jobService;
        this.taskService = taskService;
    }

    @Transactional
    public RatingResponse crear(
            Integer currentUserId,
            Integer idTrabajo,
            CreateRatingRequest request
    ) {
        TrabajoAsignado trabajo = jobService.findJobEntity(idTrabajo);
        jobService.requireJobParticipant(trabajo, currentUserId);

        if (!ESTADO_TRABAJO_FINALIZADO.equals(trabajo.getEstadoTrabajo())) {
            throw new ResourceConflictException(
                    "Solo se pueden calificar trabajos finalizados."
            );
        }

        Tarea tarea = taskService.findTaskEntity(trabajo.getIdTarea());
        Integer idCalificado = currentUserId.equals(trabajo.getIdEstudiante())
                ? tarea.getIdCliente()
                : trabajo.getIdEstudiante();

        if (idCalificado.equals(currentUserId)) {
            throw new ForbiddenOperationException("No puedes calificarte a ti mismo.");
        }

        if (calificacionRepository.existsByIdTrabajoAndIdCalificador(idTrabajo, currentUserId)) {
            throw new ResourceConflictException("Ya calificaste este trabajo.");
        }

        Calificacion calificacion = new Calificacion();
        calificacion.setIdTrabajo(idTrabajo);
        calificacion.setIdCalificador(currentUserId);
        calificacion.setIdCalificado(idCalificado);
        calificacion.setPuntuacion(request.puntuacion());
        calificacion.setComentario(normalizeComment(request.comentario()));
        calificacion.setFechaCalificacion(LocalDateTime.now());

        return RatingResponse.fromEntity(calificacionRepository.save(calificacion));
    }

    @Transactional(readOnly = true)
    public List<RatingResponse> listarPorTrabajo(Integer currentUserId, Integer idTrabajo) {
        TrabajoAsignado trabajo = jobService.findJobEntity(idTrabajo);
        jobService.requireJobParticipant(trabajo, currentUserId);
        return calificacionRepository
                .findByIdTrabajoOrderByFechaCalificacionDesc(idTrabajo)
                .stream()
                .map(RatingResponse::fromEntity)
                .toList();
    }

    private String normalizeComment(String comentario) {
        if (comentario == null) {
            return null;
        }
        String trimmed = comentario.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
