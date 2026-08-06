package com.t4kash.api.marketplace.service;

import com.t4kash.api.exception.ForbiddenOperationException;
import com.t4kash.api.exception.ResourceConflictException;
import com.t4kash.api.exception.ResourceNotFoundException;
import com.t4kash.api.marketplace.dto.CategoriaResponse;
import com.t4kash.api.marketplace.dto.CreateTaskRequest;
import com.t4kash.api.marketplace.dto.TaskResponse;
import com.t4kash.api.marketplace.entity.Postulacion;
import com.t4kash.api.marketplace.entity.Tarea;
import com.t4kash.api.marketplace.repository.CategoriaTareaRepository;
import com.t4kash.api.marketplace.repository.PostulacionRepository;
import com.t4kash.api.marketplace.repository.TareaRepository;
import com.t4kash.api.marketplace.repository.TrabajoAsignadoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Owns the lifecycle of a Tarea (publication): categories, CRUD, cancellation
 * and the expiration sweep. Other marketplace services depend on this one for
 * task lookups and ownership checks instead of talking to TareaRepository
 * directly, so those rules stay in one place.
 */
@Service
public class TaskService {
    private static final String ESTADO_TAREA_PUBLICADA = "PUBLICADA";
    private static final String ESTADO_TAREA_ASIGNADA = "ASIGNADA";
    private static final String ESTADO_TAREA_CERRADA = "CERRADA";
    private static final String ESTADO_TAREA_CANCELADA = "CANCELADA";
    private static final String ESTADO_POSTULACION_PENDIENTE = "PENDIENTE";
    private static final String ESTADO_POSTULACION_CANCELADA_TAREA = "CANCELADA_TAREA";
    private static final String MODALIDAD_REMOTA = "REMOTA";
    private static final Set<String> MODALIDADES_VALIDAS =
            Set.of(MODALIDAD_REMOTA, "PRESENCIAL", "HIBRIDA");

    private final CategoriaTareaRepository categoriaRepository;
    private final TareaRepository tareaRepository;
    private final PostulacionRepository postulacionRepository;
    private final TrabajoAsignadoRepository trabajoRepository;

    public TaskService(
            CategoriaTareaRepository categoriaRepository,
            TareaRepository tareaRepository,
            PostulacionRepository postulacionRepository,
            TrabajoAsignadoRepository trabajoRepository
    ) {
        this.categoriaRepository = categoriaRepository;
        this.tareaRepository = tareaRepository;
        this.postulacionRepository = postulacionRepository;
        this.trabajoRepository = trabajoRepository;
    }

    @Transactional(readOnly = true)
    public List<CategoriaResponse> listCategories() {
        return categoriaRepository.findByEstadoTrueOrderByNombreCategoriaAsc()
                .stream()
                .map(CategoriaResponse::fromEntity)
                .toList();
    }

    @Transactional
    public List<TaskResponse> listTasks() {
        List<Tarea> tareas = tareaRepository.findAllByOrderByFechaPublicacionDesc();
        tareas.forEach(tarea -> closeExpiredTask(tarea, LocalDateTime.now()));
        return tareas
                .stream()
                .map(TaskResponse::fromEntity)
                .toList();
    }

    @Transactional
    public List<TaskResponse> listTasksForAdmin() {
        return listTasks();
    }

    @Transactional
    public TaskResponse getTask(Integer idTarea) {
        Tarea tarea = findTaskEntity(idTarea);
        closeExpiredTask(tarea, LocalDateTime.now());
        return TaskResponse.fromEntity(tarea);
    }

    @Transactional
    public TaskResponse createTask(Integer currentUserId, CreateTaskRequest request) {
        if (!categoriaRepository.existsById(request.idCategoria())) {
            throw new ResourceNotFoundException("La categoria indicada no existe.");
        }
        validateTaskDates(request, LocalDateTime.now());

        Tarea tarea = new Tarea();
        tarea.setTitulo(request.titulo().trim());
        tarea.setDescripcion(request.descripcion().trim());
        tarea.setPresupuesto(request.presupuesto());
        tarea.setFechaPublicacion(LocalDateTime.now());
        tarea.setFechaLimitePostulacion(request.fechaLimitePostulacion());
        tarea.setFechaLimite(request.fechaLimite());
        tarea.setEstadoTarea(ESTADO_TAREA_PUBLICADA);
        tarea.setIdCategoria(request.idCategoria());
        tarea.setIdCliente(currentUserId);
        tarea.setTipoOportunidad(request.tipoOportunidad().trim());
        String modalidad = normalizeModality(request.modalidad());
        tarea.setModalidad(modalidad);
        tarea.setVisibilidad(request.visibilidad() == null || request.visibilidad().isBlank()
                ? "PUBLICA"
                : request.visibilidad().trim().toUpperCase(Locale.ROOT));
        applyLocation(tarea, request, modalidad);

        return TaskResponse.fromEntity(tareaRepository.save(tarea));
    }

    @Transactional
    public TaskResponse updateTask(
            Integer currentUserId,
            Integer idTarea,
            CreateTaskRequest request
    ) {
        Tarea tarea = findTaskEntity(idTarea);
        requireTaskOwner(tarea, currentUserId);
        requireEditableTask(tarea);
        if (!categoriaRepository.existsById(request.idCategoria())) {
            throw new ResourceNotFoundException("La categoria indicada no existe.");
        }
        validateTaskDates(request, LocalDateTime.now());

        tarea.setTitulo(request.titulo().trim());
        tarea.setDescripcion(request.descripcion().trim());
        tarea.setPresupuesto(request.presupuesto());
        tarea.setFechaLimitePostulacion(request.fechaLimitePostulacion());
        tarea.setFechaLimite(request.fechaLimite());
        tarea.setIdCategoria(request.idCategoria());
        tarea.setTipoOportunidad(request.tipoOportunidad().trim());
        String modalidad = normalizeModality(request.modalidad());
        tarea.setModalidad(modalidad);
        tarea.setVisibilidad(request.visibilidad() == null || request.visibilidad().isBlank()
                ? "PUBLICA"
                : request.visibilidad().trim().toUpperCase(Locale.ROOT));
        applyLocation(tarea, request, modalidad);
        return TaskResponse.fromEntity(tareaRepository.save(tarea));
    }

    @Transactional
    public TaskResponse cancelTask(Integer currentUserId, Integer idTarea) {
        Tarea tarea = findTaskEntity(idTarea);
        requireTaskOwner(tarea, currentUserId);
        requireEditableTask(tarea);
        return cancelTaskAndApplications(tarea);
    }

    @Transactional
    public TaskResponse cancelTaskAsAdmin(Integer idTarea) {
        Tarea tarea = findTaskEntity(idTarea);
        if (ESTADO_TAREA_CANCELADA.equals(tarea.getEstadoTarea())) {
            throw new ResourceConflictException("La publicacion ya se encuentra cancelada.");
        }
        if (ESTADO_TAREA_ASIGNADA.equals(tarea.getEstadoTarea())
                || trabajoRepository.findByIdTarea(idTarea).isPresent()) {
            throw new ResourceConflictException(
                    "No se puede retirar una publicacion con un trabajo asignado."
            );
        }
        return cancelTaskAndApplications(tarea);
    }

    /** Looks up a Tarea or throws - shared by the other marketplace services. */
    public Tarea findTaskEntity(Integer idTarea) {
        return tareaRepository.findById(idTarea)
                .orElseThrow(() -> new ResourceNotFoundException("La tarea indicada no existe."));
    }

    /** Persists a Tarea mutated by another marketplace service (e.g. accepting an application). */
    public Tarea save(Tarea tarea) {
        return tareaRepository.save(tarea);
    }

    /** Throws ForbiddenOperationException unless currentUserId owns the task. */
    public void requireTaskOwner(Tarea tarea, Integer currentUserId) {
        if (!tarea.getIdCliente().equals(currentUserId)) {
            throw new ForbiddenOperationException(
                    "Solo el propietario de la tarea puede realizar esta accion."
            );
        }
    }

    /**
     * Closes a task past its application deadline. Mutates {@code tarea} in
     * place and returns whether it was closed, so callers (e.g. applyToTask)
     * can reject actions against a task that just expired.
     */
    public boolean closeExpiredTask(Tarea tarea, LocalDateTime now) {
        LocalDateTime deadline = tarea.getFechaLimitePostulacion();
        if (
                ESTADO_TAREA_PUBLICADA.equals(tarea.getEstadoTarea()) &&
                deadline != null &&
                !deadline.isAfter(now)
        ) {
            tarea.setEstadoTarea(ESTADO_TAREA_CERRADA);
            return true;
        }
        return false;
    }

    private TaskResponse cancelTaskAndApplications(Tarea tarea) {
        tarea.setEstadoTarea(ESTADO_TAREA_CANCELADA);
        List<Postulacion> pending = postulacionRepository
                .findByIdTareaAndEstadoPostulacion(
                        tarea.getIdTarea(),
                        ESTADO_POSTULACION_PENDIENTE
                );
        pending.forEach(application ->
                application.setEstadoPostulacion(ESTADO_POSTULACION_CANCELADA_TAREA)
        );
        postulacionRepository.saveAll(pending);
        return TaskResponse.fromEntity(tareaRepository.save(tarea));
    }

    private void requireEditableTask(Tarea tarea) {
        if (!ESTADO_TAREA_PUBLICADA.equals(tarea.getEstadoTarea())) {
            throw new ResourceConflictException(
                    "Solo se pueden editar o cancelar publicaciones activas."
            );
        }
        if (trabajoRepository.findByIdTarea(tarea.getIdTarea()).isPresent()) {
            throw new ResourceConflictException(
                    "La publicacion ya tiene un estudiante asignado."
            );
        }
    }

    private String normalizeModality(String value) {
        String modalidad = value == null || value.isBlank()
                ? MODALIDAD_REMOTA
                : value.trim().toUpperCase(Locale.ROOT);
        if ("REMOTO".equals(modalidad)) {
            modalidad = MODALIDAD_REMOTA;
        }
        if (!MODALIDADES_VALIDAS.contains(modalidad)) {
            throw new IllegalArgumentException(
                    "La modalidad debe ser REMOTA, PRESENCIAL o HIBRIDA."
            );
        }
        return modalidad;
    }

    private void applyLocation(
            Tarea tarea,
            CreateTaskRequest request,
            String modalidad
    ) {
        if (MODALIDAD_REMOTA.equals(modalidad)) {
            tarea.setDireccionReferencia(null);
            tarea.setLatitud(null);
            tarea.setLongitud(null);
            return;
        }

        if (request.latitud() == null || request.longitud() == null) {
            throw new IllegalArgumentException(
                    "Las tareas presenciales o hibridas requieren latitud y longitud."
            );
        }

        tarea.setDireccionReferencia(
                request.direccionReferencia() == null || request.direccionReferencia().isBlank()
                        ? null
                        : request.direccionReferencia().trim()
        );
        tarea.setLatitud(request.latitud());
        tarea.setLongitud(request.longitud());
    }

    private void validateTaskDates(
            CreateTaskRequest request,
            LocalDateTime now
    ) {
        LocalDateTime applicationDeadline = request.fechaLimitePostulacion();
        LocalDateTime taskDeadline = request.fechaLimite();
        if ((applicationDeadline == null) != (taskDeadline == null)) {
            throw new IllegalArgumentException(
                    "El cierre de postulaciones y la fecha del trabajo deben enviarse juntos."
            );
        }
        if (applicationDeadline != null && !applicationDeadline.isAfter(now)) {
            throw new IllegalArgumentException(
                    "La fecha limite de postulacion debe ser futura."
            );
        }
        if (
                applicationDeadline != null &&
                taskDeadline != null &&
                !taskDeadline.isAfter(applicationDeadline)
        ) {
            throw new IllegalArgumentException(
                    "La fecha limite del trabajo debe ser posterior al cierre de postulaciones."
            );
        }
    }
}
