package com.t4kash.api.marketplace.service;

import com.t4kash.api.exception.ForbiddenOperationException;
import com.t4kash.api.exception.ResourceConflictException;
import com.t4kash.api.exception.ResourceNotFoundException;
import com.t4kash.api.marketplace.dto.ApplicationResponse;
import com.t4kash.api.marketplace.dto.CategoriaResponse;
import com.t4kash.api.marketplace.dto.CreateApplicationRequest;
import com.t4kash.api.marketplace.dto.CreateDeliveryRequest;
import com.t4kash.api.marketplace.dto.CreateTaskRequest;
import com.t4kash.api.marketplace.dto.DeliveryResponse;
import com.t4kash.api.marketplace.dto.JobResponse;
import com.t4kash.api.marketplace.dto.TaskResponse;
import com.t4kash.api.marketplace.entity.Entrega;
import com.t4kash.api.marketplace.entity.Postulacion;
import com.t4kash.api.marketplace.entity.Tarea;
import com.t4kash.api.marketplace.entity.TrabajoAsignado;
import com.t4kash.api.marketplace.repository.CategoriaTareaRepository;
import com.t4kash.api.marketplace.repository.EntregaRepository;
import com.t4kash.api.marketplace.repository.PostulacionRepository;
import com.t4kash.api.marketplace.repository.TareaRepository;
import com.t4kash.api.marketplace.repository.TrabajoAsignadoRepository;
import com.t4kash.api.marketplace.repository.UsuarioEstudianteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class MarketplaceService {
    private static final String ESTADO_TAREA_PUBLICADA = "PUBLICADA";
    private static final String ESTADO_TAREA_ASIGNADA = "ASIGNADA";
    private static final String ESTADO_TAREA_CERRADA = "CERRADA";
    private static final String ESTADO_TAREA_CANCELADA = "CANCELADA";
    private static final String ESTADO_POSTULACION_PENDIENTE = "PENDIENTE";
    private static final String ESTADO_POSTULACION_ACEPTADA = "ACEPTADA";
    private static final String ESTADO_POSTULACION_RECHAZADA = "RECHAZADA";
    private static final String ESTADO_POSTULACION_CANCELADA_LIMITE = "CANCELADA_LIMITE";
    private static final String ESTADO_POSTULACION_CANCELADA_TAREA = "CANCELADA_TAREA";
    private static final String ESTADO_TRABAJO_EN_PROCESO = "EN_PROCESO";
    private static final String ESTADO_TRABAJO_FINALIZADO = "FINALIZADO";
    private static final String ESTADO_ENTREGA_ENVIADA = "ENVIADA";
    private static final String ESTADO_ENTREGA_APROBADA = "APROBADA";
    private static final String MODALIDAD_REMOTA = "REMOTA";
    private static final Set<String> MODALIDADES_VALIDAS =
            Set.of(MODALIDAD_REMOTA, "PRESENCIAL", "HIBRIDA");
    private static final int MAX_ACTIVE_JOBS = 2;
    private static final int MAX_APPLICATION_ATTEMPTS = 3;

    private final CategoriaTareaRepository categoriaRepository;
    private final TareaRepository tareaRepository;
    private final PostulacionRepository postulacionRepository;
    private final TrabajoAsignadoRepository trabajoRepository;
    private final EntregaRepository entregaRepository;
    private final UsuarioEstudianteRepository estudianteRepository;

    public MarketplaceService(
            CategoriaTareaRepository categoriaRepository,
            TareaRepository tareaRepository,
            PostulacionRepository postulacionRepository,
            TrabajoAsignadoRepository trabajoRepository,
            EntregaRepository entregaRepository,
            UsuarioEstudianteRepository estudianteRepository
    ) {
        this.categoriaRepository = categoriaRepository;
        this.tareaRepository = tareaRepository;
        this.postulacionRepository = postulacionRepository;
        this.trabajoRepository = trabajoRepository;
        this.entregaRepository = entregaRepository;
        this.estudianteRepository = estudianteRepository;
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
        closeExpiredTasks(tareas, LocalDateTime.now());
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
        Tarea tarea = findTask(idTarea);
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
        Tarea tarea = findTask(idTarea);
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
        Tarea tarea = findTask(idTarea);
        requireTaskOwner(tarea, currentUserId);
        requireEditableTask(tarea);
        return cancelTaskAndApplications(tarea);
    }

    @Transactional
    public TaskResponse cancelTaskAsAdmin(Integer idTarea) {
        Tarea tarea = findTask(idTarea);
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

    @Transactional(readOnly = true)
    public List<ApplicationResponse> listApplications(Integer currentUserId, Integer idTarea) {
        Tarea tarea = findTask(idTarea);
        requireTaskOwner(tarea, currentUserId);
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
        Tarea tarea = findTask(idTarea);
        if (closeExpiredTask(tarea, LocalDateTime.now())) {
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

        return ApplicationResponse.fromEntity(postulacionRepository.save(postulacion));
    }

    @Transactional
    public JobResponse acceptApplication(Integer currentUserId, Integer idPostulacion) {
        Postulacion postulacion = findApplication(idPostulacion);
        Tarea tarea = findTask(postulacion.getIdTarea());
        requireTaskOwner(tarea, currentUserId);
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

        tareaRepository.save(tarea);
        postulacionRepository.save(postulacion);
        rejectRemainingApplications(postulacion);
        TrabajoAsignado savedJob = trabajoRepository.save(trabajo);
        if (currentJobs + 1 >= MAX_ACTIVE_JOBS) {
            cancelPendingApplicationsForLimit(
                    postulacion.getIdEstudiante(),
                    postulacion.getIdPostulacion()
            );
        }
        return JobResponse.fromEntity(savedJob);
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

    private void closeExpiredTasks(
            List<Tarea> tareas,
            LocalDateTime now
    ) {
        tareas.forEach(tarea -> closeExpiredTask(tarea, now));
    }

    private boolean closeExpiredTask(
            Tarea tarea,
            LocalDateTime now
    ) {
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

    @Transactional
    public ApplicationResponse rejectApplication(Integer currentUserId, Integer idPostulacion) {
        Postulacion postulacion = findApplication(idPostulacion);
        requireTaskOwner(findTask(postulacion.getIdTarea()), currentUserId);
        if (!ESTADO_POSTULACION_PENDIENTE.equals(postulacion.getEstadoPostulacion())) {
            throw new ResourceConflictException("Solo se pueden rechazar postulaciones pendientes.");
        }
        postulacion.setEstadoPostulacion(ESTADO_POSTULACION_RECHAZADA);
        return ApplicationResponse.fromEntity(postulacionRepository.save(postulacion));
    }

    @Transactional(readOnly = true)
    public List<JobResponse> listJobs(Integer currentUserId) {
        return trabajoRepository.findVisibleToUser(currentUserId)
                .stream()
                .map(JobResponse::fromEntity)
                .toList();
    }

    @Transactional
    public DeliveryResponse createDelivery(
            Integer currentUserId,
            Integer idTrabajo,
            CreateDeliveryRequest request
    ) {
        TrabajoAsignado trabajo = findJob(idTrabajo);
        requireAssignedStudent(trabajo, currentUserId);
        if (!ESTADO_TRABAJO_EN_PROCESO.equals(trabajo.getEstadoTrabajo())) {
            throw new ResourceConflictException("Solo se pueden enviar entregas para trabajos en proceso.");
        }

        Entrega entrega = new Entrega();
        entrega.setIdTrabajo(idTrabajo);
        entrega.setDescripcionEntrega(request.descripcionEntrega().trim());
        entrega.setFechaEntrega(LocalDateTime.now());
        entrega.setEstadoEntrega(ESTADO_ENTREGA_ENVIADA);

        return DeliveryResponse.fromEntity(entregaRepository.save(entrega));
    }

    @Transactional(readOnly = true)
    public List<DeliveryResponse> listDeliveries(Integer currentUserId, Integer idTrabajo) {
        TrabajoAsignado trabajo = findJob(idTrabajo);
        requireJobParticipant(trabajo, currentUserId);
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

        TrabajoAsignado trabajo = findJob(entrega.getIdTrabajo());
        requireTaskOwner(findTask(trabajo.getIdTarea()), currentUserId);
        entrega.setEstadoEntrega(ESTADO_ENTREGA_APROBADA);
        trabajo.setEstadoTrabajo(ESTADO_TRABAJO_FINALIZADO);

        trabajoRepository.save(trabajo);
        return DeliveryResponse.fromEntity(entregaRepository.save(entrega));
    }

    private Tarea findTask(Integer idTarea) {
        return tareaRepository.findById(idTarea)
                .orElseThrow(() -> new ResourceNotFoundException("La tarea indicada no existe."));
    }

    private void requireTaskOwner(Tarea tarea, Integer currentUserId) {
        if (!tarea.getIdCliente().equals(currentUserId)) {
            throw new ForbiddenOperationException(
                    "Solo el propietario de la tarea puede realizar esta accion."
            );
        }
    }

    private void requireAssignedStudent(
            TrabajoAsignado trabajo,
            Integer currentUserId
    ) {
        if (!trabajo.getIdEstudiante().equals(currentUserId)) {
            throw new ForbiddenOperationException(
                    "Solo el estudiante asignado puede realizar esta accion."
            );
        }
    }

    private void requireJobParticipant(
            TrabajoAsignado trabajo,
            Integer currentUserId
    ) {
        Tarea tarea = findTask(trabajo.getIdTarea());
        boolean isParticipant =
                trabajo.getIdEstudiante().equals(currentUserId) ||
                tarea.getIdCliente().equals(currentUserId);
        if (!isParticipant) {
            throw new ForbiddenOperationException(
                    "Solo los participantes del trabajo pueden consultar esta informacion."
            );
        }
    }

    private Postulacion findApplication(Integer idPostulacion) {
        return postulacionRepository.findById(idPostulacion)
                .orElseThrow(() -> new ResourceNotFoundException("La postulacion indicada no existe."));
    }

    private TrabajoAsignado findJob(Integer idTrabajo) {
        return trabajoRepository.findById(idTrabajo)
                .orElseThrow(() -> new ResourceNotFoundException("El trabajo indicado no existe."));
    }

    private Entrega findDelivery(Integer idEntrega) {
        return entregaRepository.findById(idEntrega)
                .orElseThrow(() -> new ResourceNotFoundException("La entrega indicada no existe."));
    }
}
