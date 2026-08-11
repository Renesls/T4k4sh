package com.t4kash.api.marketplace.service;

import com.t4kash.api.exception.ForbiddenOperationException;
import com.t4kash.api.exception.ResourceConflictException;
import com.t4kash.api.exception.ResourceNotFoundException;
import com.t4kash.api.config.PaginationSupport;
import com.t4kash.api.marketplace.dto.CategoriaResponse;
import com.t4kash.api.marketplace.dto.CreateTaskRequest;
import com.t4kash.api.marketplace.dto.QuickTaskResponse;
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
import java.time.Duration;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Gestiona el ciclo de una Tarea: categorias, operaciones CRUD, cancelacion y
 * vencimiento. Los demas servicios del marketplace delegan aqui las consultas
 * y validaciones de propiedad para mantener las reglas en un solo lugar.
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
    public static final String TIPO_TAREA_RAPIDA = "RAPIDA";
    private static final String MODALIDAD_PRESENCIAL = "PRESENCIAL";
    private static final double RADIO_RAPIDO_MINIMO_KM = 0.25;
    private static final double RADIO_RAPIDO_MAXIMO_KM = 5.0;
    private static final long HORAS_DISPONIBLE_TAREA_RAPIDA = 24;
    public static final long HORAS_ENTREGA_TAREA_RAPIDA = 3;
    private static final BigDecimal PAGO_MAXIMO_TAREA_RAPIDA = new BigDecimal("1000.00");
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
        return listTasks(0, PaginationSupport.DEFAULT_SIZE);
    }

    @Transactional
    public List<TaskResponse> listTasks(int page, int size) {
        tareaRepository.closeExpiredPublishedTasks(LocalDateTime.now());
        return tareaRepository
                .findAllByOrderByFechaPublicacionDesc(PaginationSupport.page(page, size))
                .stream()
                .map(TaskResponse::fromEntity)
                .toList();
    }

    @Transactional
    public List<TaskResponse> listTasksForAdmin() {
        return listTasks();
    }

    @Transactional
    public List<TaskResponse> listTasksForAdmin(int page, int size) {
        return listTasks(page, size);
    }

    @Transactional
    public List<QuickTaskResponse> listNearbyQuickTasks(
            Integer currentUserId,
            double latitude,
            double longitude,
            double radiusKm
    ) {
        validateCoordinates(latitude, longitude);
        if (radiusKm < RADIO_RAPIDO_MINIMO_KM || radiusKm > RADIO_RAPIDO_MAXIMO_KM) {
            throw new IllegalArgumentException(
                    "El radio de busqueda debe estar entre 0.25 y 5 kilometros."
            );
        }

        LocalDateTime now = LocalDateTime.now();
        double latitudeDelta = radiusKm / 111.32;
        double longitudeScale = Math.max(
                0.01,
                Math.cos(Math.toRadians(latitude))
        );
        double longitudeDelta = radiusKm / (111.32 * longitudeScale);
        return tareaRepository
                .findQuickTasksWithinBounds(
                        TIPO_TAREA_RAPIDA,
                        ESTADO_TAREA_PUBLICADA,
                        BigDecimal.valueOf(Math.max(-90, latitude - latitudeDelta)),
                        BigDecimal.valueOf(Math.min(90, latitude + latitudeDelta)),
                        BigDecimal.valueOf(Math.max(-180, longitude - longitudeDelta)),
                        BigDecimal.valueOf(Math.min(180, longitude + longitudeDelta))
                )
                .stream()
                .filter(tarea -> !closeExpiredTask(tarea, now))
                .filter(tarea -> !tarea.getIdCliente().equals(currentUserId))
                .filter(tarea -> tarea.getLatitud() != null && tarea.getLongitud() != null)
                .map(tarea -> toQuickTask(tarea, latitude, longitude, now))
                .filter(task -> task.distanciaKm() <= radiusKm)
                .sorted(Comparator.comparingDouble(QuickTaskResponse::distanciaKm))
                .limit(50)
                .toList();
    }

    @Transactional
    public TaskResponse getTask(Integer idTarea) {
        Tarea tarea = findTaskEntity(idTarea);
        closeExpiredTask(tarea, LocalDateTime.now());
        return TaskResponse.fromEntity(tarea);
    }

    @Transactional
    public TaskResponse createTask(Integer currentUserId, CreateTaskRequest request) {
        if (!categoriaRepository.existsByIdCategoriaAndEstadoTrue(request.idCategoria())) {
            throw new ResourceNotFoundException("La categoria indicada no existe o esta inactiva.");
        }
        LocalDateTime now = LocalDateTime.now();
        String opportunityType = normalizeOpportunityType(request.tipoOportunidad());
        String modalidad = normalizeModality(request.modalidad());
        validateQuickTask(request, opportunityType, modalidad);
        LocalDateTime applicationDeadline = request.fechaLimitePostulacion();
        LocalDateTime taskDeadline = request.fechaLimite();
        if (TIPO_TAREA_RAPIDA.equals(opportunityType)) {
            applicationDeadline = now.plusHours(HORAS_DISPONIBLE_TAREA_RAPIDA);
            taskDeadline = applicationDeadline.plusHours(HORAS_ENTREGA_TAREA_RAPIDA);
        } else {
            validateTaskDates(request, now);
        }

        Tarea tarea = new Tarea();
        tarea.setTitulo(request.titulo().trim());
        tarea.setDescripcion(request.descripcion().trim());
        tarea.setPresupuesto(request.presupuesto());
        tarea.setFechaPublicacion(now);
        tarea.setFechaLimitePostulacion(applicationDeadline);
        tarea.setFechaLimite(taskDeadline);
        tarea.setEstadoTarea(ESTADO_TAREA_PUBLICADA);
        tarea.setIdCategoria(request.idCategoria());
        tarea.setIdCliente(currentUserId);
        tarea.setTipoOportunidad(opportunityType);
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
        if (!categoriaRepository.existsByIdCategoriaAndEstadoTrue(request.idCategoria())) {
            throw new ResourceNotFoundException("La categoria indicada no existe o esta inactiva.");
        }
        LocalDateTime now = LocalDateTime.now();
        String opportunityType = normalizeOpportunityType(request.tipoOportunidad());
        String modalidad = normalizeModality(request.modalidad());
        validateQuickTask(request, opportunityType, modalidad);
        LocalDateTime applicationDeadline = request.fechaLimitePostulacion();
        LocalDateTime taskDeadline = request.fechaLimite();
        if (TIPO_TAREA_RAPIDA.equals(opportunityType)) {
            boolean alreadyQuick = TIPO_TAREA_RAPIDA.equalsIgnoreCase(
                    tarea.getTipoOportunidad()
            );
            if (alreadyQuick && tarea.getFechaLimitePostulacion() != null) {
                applicationDeadline = tarea.getFechaLimitePostulacion();
                taskDeadline = tarea.getFechaLimite();
            } else {
                applicationDeadline = now.plusHours(HORAS_DISPONIBLE_TAREA_RAPIDA);
                taskDeadline = applicationDeadline.plusHours(HORAS_ENTREGA_TAREA_RAPIDA);
            }
        } else {
            validateTaskDates(request, now);
        }

        tarea.setTitulo(request.titulo().trim());
        tarea.setDescripcion(request.descripcion().trim());
        tarea.setPresupuesto(request.presupuesto());
        tarea.setFechaLimitePostulacion(applicationDeadline);
        tarea.setFechaLimite(taskDeadline);
        tarea.setIdCategoria(request.idCategoria());
        tarea.setTipoOportunidad(opportunityType);
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

    /** Busca una tarea o informa que no existe para los demas servicios. */
    public Tarea findTaskEntity(Integer idTarea) {
        return tareaRepository.findById(idTarea)
                .orElseThrow(() -> new ResourceNotFoundException("La tarea indicada no existe."));
    }

    /** Bloquea la fila mientras una tarea rapida se asigna a un estudiante. */
    public Tarea findTaskEntityForUpdate(Integer idTarea) {
        return tareaRepository.findByIdForUpdate(idTarea)
                .orElseThrow(() -> new ResourceNotFoundException("La tarea indicada no existe."));
    }

    /** Guarda una tarea modificada por otro servicio del marketplace. */
    public Tarea save(Tarea tarea) {
        return tareaRepository.save(tarea);
    }

    /** Verifica que el usuario actual sea propietario de la tarea. */
    public void requireTaskOwner(Tarea tarea, Integer currentUserId) {
        if (!tarea.getIdCliente().equals(currentUserId)) {
            throw new ForbiddenOperationException(
                    "Solo el propietario de la tarea puede realizar esta accion."
            );
        }
    }

    /**
     * Cierra una tarea cuando vence su plazo de postulacion y devuelve si hubo
     * un cambio para que el servicio solicitante pueda rechazar la operacion.
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

    private String normalizeOpportunityType(String value) {
        return value == null || value.isBlank()
                ? "TAREA"
                : value.trim().toUpperCase(Locale.ROOT);
    }

    private void validateQuickTask(
            CreateTaskRequest request,
            String opportunityType,
            String modality
    ) {
        if (!TIPO_TAREA_RAPIDA.equals(opportunityType)) {
            return;
        }
        if (!MODALIDAD_PRESENCIAL.equals(modality)) {
            throw new IllegalArgumentException(
                    "Las tareas rapidas deben ser presenciales."
            );
        }
        if (request.presupuesto() == null || request.presupuesto().signum() <= 0) {
            throw new IllegalArgumentException(
                    "Las tareas rapidas requieren un pago mayor que cero."
            );
        }
        if (request.presupuesto().compareTo(PAGO_MAXIMO_TAREA_RAPIDA) > 0) {
            throw new IllegalArgumentException(
                    "El pago de una tarea rapida no puede superar C$1,000."
            );
        }
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

    private QuickTaskResponse toQuickTask(
            Tarea tarea,
            double latitude,
            double longitude,
            LocalDateTime now
    ) {
        double distance = distanceInKilometers(
                latitude,
                longitude,
                tarea.getLatitud().doubleValue(),
                tarea.getLongitud().doubleValue()
        );
        LocalDateTime deadline = tarea.getFechaLimitePostulacion();
        long remaining = deadline == null
                ? 0
                : Math.max(0, Duration.between(now, deadline).toSeconds());
        return new QuickTaskResponse(
                TaskResponse.fromEntity(tarea),
                Math.round(distance * 100.0) / 100.0,
                remaining
        );
    }

    private void validateCoordinates(double latitude, double longitude) {
        if (latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) {
            throw new IllegalArgumentException("La ubicacion indicada no es valida.");
        }
    }

    private double distanceInKilometers(
            double originLatitude,
            double originLongitude,
            double destinationLatitude,
            double destinationLongitude
    ) {
        double earthRadiusKm = 6371.0088;
        double latitudeDelta = Math.toRadians(destinationLatitude - originLatitude);
        double longitudeDelta = Math.toRadians(destinationLongitude - originLongitude);
        double originRadians = Math.toRadians(originLatitude);
        double destinationRadians = Math.toRadians(destinationLatitude);
        double a = Math.sin(latitudeDelta / 2) * Math.sin(latitudeDelta / 2)
                + Math.cos(originRadians) * Math.cos(destinationRadians)
                * Math.sin(longitudeDelta / 2) * Math.sin(longitudeDelta / 2);
        return earthRadiusKm * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }
}
