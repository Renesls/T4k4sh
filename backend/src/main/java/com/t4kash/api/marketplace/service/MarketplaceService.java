package com.t4kash.api.marketplace.service;

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
import com.t4kash.api.marketplace.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class MarketplaceService {
    private static final String ESTADO_TAREA_PUBLICADA = "PUBLICADA";
    private static final String ESTADO_TAREA_ASIGNADA = "ASIGNADA";
    private static final String ESTADO_POSTULACION_PENDIENTE = "PENDIENTE";
    private static final String ESTADO_POSTULACION_ACEPTADA = "ACEPTADA";
    private static final String ESTADO_POSTULACION_RECHAZADA = "RECHAZADA";
    private static final String ESTADO_TRABAJO_EN_PROCESO = "EN_PROCESO";
    private static final String ESTADO_TRABAJO_FINALIZADO = "FINALIZADO";
    private static final String ESTADO_ENTREGA_ENVIADA = "ENVIADA";
    private static final String ESTADO_ENTREGA_APROBADA = "APROBADA";

    private final CategoriaTareaRepository categoriaRepository;
    private final TareaRepository tareaRepository;
    private final PostulacionRepository postulacionRepository;
    private final TrabajoAsignadoRepository trabajoRepository;
    private final EntregaRepository entregaRepository;
    private final UsuarioRepository usuarioRepository;
    private final UsuarioEstudianteRepository estudianteRepository;

    public MarketplaceService(
            CategoriaTareaRepository categoriaRepository,
            TareaRepository tareaRepository,
            PostulacionRepository postulacionRepository,
            TrabajoAsignadoRepository trabajoRepository,
            EntregaRepository entregaRepository,
            UsuarioRepository usuarioRepository,
            UsuarioEstudianteRepository estudianteRepository
    ) {
        this.categoriaRepository = categoriaRepository;
        this.tareaRepository = tareaRepository;
        this.postulacionRepository = postulacionRepository;
        this.trabajoRepository = trabajoRepository;
        this.entregaRepository = entregaRepository;
        this.usuarioRepository = usuarioRepository;
        this.estudianteRepository = estudianteRepository;
    }

    @Transactional(readOnly = true)
    public List<CategoriaResponse> listCategories() {
        return categoriaRepository.findByEstadoTrueOrderByNombreCategoriaAsc()
                .stream()
                .map(CategoriaResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> listTasks() {
        return tareaRepository.findAllByOrderByFechaPublicacionDesc()
                .stream()
                .map(TaskResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public TaskResponse getTask(Integer idTarea) {
        return TaskResponse.fromEntity(findTask(idTarea));
    }

    @Transactional
    public TaskResponse createTask(CreateTaskRequest request) {
        if (!categoriaRepository.existsById(request.idCategoria())) {
            throw new ResourceNotFoundException("La categoria indicada no existe.");
        }
        if (!usuarioRepository.existsById(request.idCliente())) {
            throw new ResourceNotFoundException("El usuario cliente indicado no existe.");
        }

        Tarea tarea = new Tarea();
        tarea.setTitulo(request.titulo().trim());
        tarea.setDescripcion(request.descripcion().trim());
        tarea.setPresupuesto(request.presupuesto());
        tarea.setFechaPublicacion(LocalDateTime.now());
        tarea.setFechaLimitePostulacion(request.fechaLimitePostulacion());
        tarea.setFechaLimite(request.fechaLimite());
        tarea.setEstadoTarea(ESTADO_TAREA_PUBLICADA);
        tarea.setIdCategoria(request.idCategoria());
        tarea.setIdCliente(request.idCliente());
        tarea.setTipoOportunidad(request.tipoOportunidad().trim());
        tarea.setModalidad(request.modalidad());
        tarea.setVisibilidad(request.visibilidad() == null || request.visibilidad().isBlank()
                ? "PUBLICA"
                : request.visibilidad().trim());

        return TaskResponse.fromEntity(tareaRepository.save(tarea));
    }

    @Transactional(readOnly = true)
    public List<ApplicationResponse> listApplications(Integer idTarea) {
        ensureTaskExists(idTarea);
        return postulacionRepository.findByIdTareaOrderByFechaPostulacionDesc(idTarea)
                .stream()
                .map(ApplicationResponse::fromEntity)
                .toList();
    }

    @Transactional
    public ApplicationResponse applyToTask(Integer idTarea, CreateApplicationRequest request) {
        Tarea tarea = findTask(idTarea);
        if (!ESTADO_TAREA_PUBLICADA.equals(tarea.getEstadoTarea())) {
            throw new ResourceConflictException("La tarea no esta disponible para nuevas postulaciones.");
        }
        if (!estudianteRepository.existsById(request.idEstudiante())) {
            throw new ResourceNotFoundException("El estudiante indicado no existe.");
        }
        postulacionRepository.findByIdTareaAndIdEstudiante(idTarea, request.idEstudiante())
                .ifPresent(existing -> {
                    throw new ResourceConflictException("El estudiante ya se postulo a esta tarea.");
                });

        Postulacion postulacion = new Postulacion();
        postulacion.setIdTarea(idTarea);
        postulacion.setIdEstudiante(request.idEstudiante());
        postulacion.setMensaje(request.mensaje());
        postulacion.setPrecioPropuesto(request.precioPropuesto());
        postulacion.setFechaPostulacion(LocalDateTime.now());
        postulacion.setEstadoPostulacion(ESTADO_POSTULACION_PENDIENTE);

        return ApplicationResponse.fromEntity(postulacionRepository.save(postulacion));
    }

    @Transactional
    public JobResponse acceptApplication(Integer idPostulacion) {
        Postulacion postulacion = findApplication(idPostulacion);
        if (!ESTADO_POSTULACION_PENDIENTE.equals(postulacion.getEstadoPostulacion())) {
            throw new ResourceConflictException("Solo se pueden aceptar postulaciones pendientes.");
        }
        if (trabajoRepository.findByIdTarea(postulacion.getIdTarea()).isPresent()) {
            throw new ResourceConflictException("Esta tarea ya tiene un trabajo asignado.");
        }

        Tarea tarea = findTask(postulacion.getIdTarea());
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
        return JobResponse.fromEntity(trabajoRepository.save(trabajo));
    }

    @Transactional
    public ApplicationResponse rejectApplication(Integer idPostulacion) {
        Postulacion postulacion = findApplication(idPostulacion);
        if (!ESTADO_POSTULACION_PENDIENTE.equals(postulacion.getEstadoPostulacion())) {
            throw new ResourceConflictException("Solo se pueden rechazar postulaciones pendientes.");
        }
        postulacion.setEstadoPostulacion(ESTADO_POSTULACION_RECHAZADA);
        return ApplicationResponse.fromEntity(postulacionRepository.save(postulacion));
    }

    @Transactional(readOnly = true)
    public List<JobResponse> listJobs() {
        return trabajoRepository.findAllByOrderByFechaInicioDesc()
                .stream()
                .map(JobResponse::fromEntity)
                .toList();
    }

    @Transactional
    public DeliveryResponse createDelivery(Integer idTrabajo, CreateDeliveryRequest request) {
        TrabajoAsignado trabajo = findJob(idTrabajo);
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
    public List<DeliveryResponse> listDeliveries(Integer idTrabajo) {
        findJob(idTrabajo);
        return entregaRepository.findByIdTrabajoOrderByFechaEntregaDesc(idTrabajo)
                .stream()
                .map(DeliveryResponse::fromEntity)
                .toList();
    }

    @Transactional
    public DeliveryResponse approveDelivery(Integer idEntrega) {
        Entrega entrega = findDelivery(idEntrega);
        if (!ESTADO_ENTREGA_ENVIADA.equals(entrega.getEstadoEntrega())) {
            throw new ResourceConflictException("Solo se pueden aprobar entregas enviadas.");
        }

        TrabajoAsignado trabajo = findJob(entrega.getIdTrabajo());
        entrega.setEstadoEntrega(ESTADO_ENTREGA_APROBADA);
        trabajo.setEstadoTrabajo(ESTADO_TRABAJO_FINALIZADO);

        trabajoRepository.save(trabajo);
        return DeliveryResponse.fromEntity(entregaRepository.save(entrega));
    }

    private Tarea findTask(Integer idTarea) {
        return tareaRepository.findById(idTarea)
                .orElseThrow(() -> new ResourceNotFoundException("La tarea indicada no existe."));
    }

    private void ensureTaskExists(Integer idTarea) {
        if (!tareaRepository.existsById(idTarea)) {
            throw new ResourceNotFoundException("La tarea indicada no existe.");
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
