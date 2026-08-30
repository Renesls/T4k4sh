package com.t4kash.api.marketplace.service;

import com.t4kash.api.exception.ForbiddenOperationException;
import com.t4kash.api.exception.ResourceConflictException;
import com.t4kash.api.marketplace.dto.CreateRatingRequest;
import com.t4kash.api.marketplace.dto.RatingResponse;
import com.t4kash.api.marketplace.entity.Calificacion;
import com.t4kash.api.marketplace.entity.Tarea;
import com.t4kash.api.marketplace.entity.TrabajoAsignado;
import com.t4kash.api.marketplace.repository.CalificacionRepository;
import com.t4kash.api.marketplace.repository.CategoriaTareaRepository;
import com.t4kash.api.marketplace.repository.PostulacionRepository;
import com.t4kash.api.marketplace.repository.TareaRepository;
import com.t4kash.api.marketplace.repository.TrabajoAsignadoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CalificacionServiceTest {
    private static final Integer STUDENT_ID = 1;
    private static final Integer CLIENT_ID = 2;
    private static final Integer OUTSIDER_ID = 99;
    private static final Integer JOB_ID = 50;
    private static final Integer TASK_ID = 10;

    @Mock
    private CalificacionRepository calificacionRepository;
    @Mock
    private CategoriaTareaRepository categoriaRepository;
    @Mock
    private TareaRepository tareaRepository;
    @Mock
    private PostulacionRepository postulacionRepository;
    @Mock
    private TrabajoAsignadoRepository trabajoRepository;

    private CalificacionService service;

    @BeforeEach
    void setUp() {
        TaskService taskService = new TaskService(
                categoriaRepository,
                tareaRepository,
                postulacionRepository,
                trabajoRepository
        );
        JobService jobService = new JobService(trabajoRepository, taskService);
        service = new CalificacionService(calificacionRepository, jobService, taskService);
    }

    @Test
    void studentRatesClientAfterJobFinished() {
        when(trabajoRepository.findById(JOB_ID)).thenReturn(Optional.of(job("FINALIZADO")));
        when(tareaRepository.findById(TASK_ID)).thenReturn(Optional.of(task()));
        when(calificacionRepository.existsByIdTrabajoAndIdCalificador(JOB_ID, STUDENT_ID))
                .thenReturn(false);
        when(calificacionRepository.save(any(Calificacion.class))).thenAnswer(invocation -> {
            Calificacion saved = invocation.getArgument(0);
            saved.setIdCalificacion(300);
            return saved;
        });

        RatingResponse response = service.crear(
                STUDENT_ID,
                JOB_ID,
                new CreateRatingRequest(5, "Excelente cliente.")
        );

        assertEquals(300, response.idCalificacion());
        assertEquals(STUDENT_ID, response.idCalificador());
        assertEquals(CLIENT_ID, response.idCalificado());
        assertEquals(5, response.puntuacion());
    }

    @Test
    void clientRatesStudentAfterJobFinished() {
        when(trabajoRepository.findById(JOB_ID)).thenReturn(Optional.of(job("FINALIZADO")));
        when(tareaRepository.findById(TASK_ID)).thenReturn(Optional.of(task()));
        when(calificacionRepository.existsByIdTrabajoAndIdCalificador(JOB_ID, CLIENT_ID))
                .thenReturn(false);
        when(calificacionRepository.save(any(Calificacion.class))).thenAnswer(invocation -> {
            Calificacion saved = invocation.getArgument(0);
            saved.setIdCalificacion(301);
            return saved;
        });

        RatingResponse response = service.crear(
                CLIENT_ID,
                JOB_ID,
                new CreateRatingRequest(4, null)
        );

        assertEquals(CLIENT_ID, response.idCalificador());
        assertEquals(STUDENT_ID, response.idCalificado());
        assertEquals(4, response.puntuacion());
    }

    @Test
    void rejectsRatingForJobNotFinished() {
        when(trabajoRepository.findById(JOB_ID)).thenReturn(Optional.of(job("EN_PROCESO")));
        when(tareaRepository.findById(TASK_ID)).thenReturn(Optional.of(task()));

        assertThrows(
                ResourceConflictException.class,
                () -> service.crear(STUDENT_ID, JOB_ID, new CreateRatingRequest(5, null))
        );
    }

    @Test
    void rejectsDuplicateRatingForSameJob() {
        when(trabajoRepository.findById(JOB_ID)).thenReturn(Optional.of(job("FINALIZADO")));
        when(tareaRepository.findById(TASK_ID)).thenReturn(Optional.of(task()));
        when(calificacionRepository.existsByIdTrabajoAndIdCalificador(JOB_ID, STUDENT_ID))
                .thenReturn(true);

        assertThrows(
                ResourceConflictException.class,
                () -> service.crear(STUDENT_ID, JOB_ID, new CreateRatingRequest(5, null))
        );
    }

    @Test
    void rejectsRatingFromNonParticipant() {
        when(trabajoRepository.findById(JOB_ID)).thenReturn(Optional.of(job("FINALIZADO")));
        when(tareaRepository.findById(TASK_ID)).thenReturn(Optional.of(task()));

        assertThrows(
                ForbiddenOperationException.class,
                () -> service.crear(OUTSIDER_ID, JOB_ID, new CreateRatingRequest(5, null))
        );
    }

    @Test
    void listsRatingsForJobParticipant() {
        when(trabajoRepository.findById(JOB_ID)).thenReturn(Optional.of(job("FINALIZADO")));
        when(tareaRepository.findById(TASK_ID)).thenReturn(Optional.of(task()));
        Calificacion existing = new Calificacion();
        existing.setIdCalificacion(1);
        existing.setIdTrabajo(JOB_ID);
        existing.setIdCalificador(STUDENT_ID);
        existing.setIdCalificado(CLIENT_ID);
        existing.setPuntuacion(5);
        existing.setFechaCalificacion(LocalDateTime.now());
        when(calificacionRepository.findByIdTrabajoOrderByFechaCalificacionDesc(JOB_ID))
                .thenReturn(List.of(existing));

        List<RatingResponse> ratings = service.listarPorTrabajo(CLIENT_ID, JOB_ID);

        assertEquals(1, ratings.size());
        assertEquals(STUDENT_ID, ratings.get(0).idCalificador());
    }

    private Tarea task() {
        Tarea task = new Tarea();
        task.setIdTarea(TASK_ID);
        task.setTitulo("Oportunidad de prueba");
        task.setDescripcion("Descripcion completa para la oportunidad de prueba.");
        task.setPresupuesto(new BigDecimal("25.00"));
        task.setFechaPublicacion(LocalDateTime.now().minusDays(3));
        task.setEstadoTarea("ASIGNADA");
        task.setIdCategoria(1);
        task.setIdCliente(CLIENT_ID);
        task.setTipoOportunidad("TAREA");
        task.setModalidad("REMOTA");
        task.setVisibilidad("PUBLICA");
        return task;
    }

    private TrabajoAsignado job(String status) {
        TrabajoAsignado job = new TrabajoAsignado();
        job.setIdTrabajo(JOB_ID);
        job.setIdTarea(TASK_ID);
        job.setIdEstudiante(STUDENT_ID);
        job.setFechaInicio(LocalDateTime.now().minusDays(2));
        job.setEstadoTrabajo(status);
        return job;
    }
}
