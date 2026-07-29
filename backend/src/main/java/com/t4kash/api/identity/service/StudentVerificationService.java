package com.t4kash.api.identity.service;

import com.t4kash.api.exception.ResourceConflictException;
import com.t4kash.api.exception.ResourceNotFoundException;
import com.t4kash.api.identity.dto.StudentVerificationResponse;
import com.t4kash.api.identity.entity.VerificacionUsuario;
import com.t4kash.api.identity.repository.UsuarioRolRepository;
import com.t4kash.api.identity.repository.VerificacionUsuarioRepository;
import com.t4kash.api.marketplace.entity.Usuario;
import com.t4kash.api.marketplace.entity.UsuarioEstudiante;
import com.t4kash.api.marketplace.repository.UsuarioEstudianteRepository;
import com.t4kash.api.marketplace.repository.UsuarioRepository;
import com.t4kash.api.marketplace.service.AttachmentService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class StudentVerificationService {
    private static final String VERIFICATION_TYPE = "PERFIL_ESTUDIANTE_MANUAL";
    private static final String PROFILE_PENDING = "PENDIENTE_REVISION";
    private static final String PROFILE_ACTIVE = "ACTIVO";
    private static final String PROFILE_REJECTED = "RECHAZADO";
    private static final String DOCUMENT_PENDING = "PENDIENTE_DOCUMENTO";
    private static final String REVIEW_PENDING = "PENDIENTE_REVISION";
    private static final String APPROVED = "APROBADO";
    private static final String REJECTED = "RECHAZADO";

    private final VerificacionUsuarioRepository verificationRepository;
    private final UsuarioEstudianteRepository studentRepository;
    private final UsuarioRepository userRepository;
    private final UsuarioRolRepository roleRepository;
    private final AttachmentService attachmentService;

    public StudentVerificationService(
            VerificacionUsuarioRepository verificationRepository,
            UsuarioEstudianteRepository studentRepository,
            UsuarioRepository userRepository,
            UsuarioRolRepository roleRepository,
            AttachmentService attachmentService
    ) {
        this.verificationRepository = verificationRepository;
        this.studentRepository = studentRepository;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.attachmentService = attachmentService;
    }

    @Transactional
    public VerificacionUsuario requireOrCreate(Integer userId) {
        UsuarioEstudiante student = studentRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No existe una solicitud de perfil estudiantil."
                ));
        if (!PROFILE_PENDING.equals(student.getEstadoPerfilEstudiante())
                && !PROFILE_REJECTED.equals(student.getEstadoPerfilEstudiante())) {
            throw new ResourceConflictException(
                    "El perfil estudiantil no requiere validacion documental."
            );
        }
        return verificationRepository
                .findFirstByIdUsuarioAndTipoVerificacionOrderByFechaSolicitudDesc(
                        userId,
                        VERIFICATION_TYPE
                )
                .filter(item -> !APPROVED.equals(item.getEstadoVerificacion()))
                .orElseGet(() -> createVerification(userId));
    }

    @Transactional
    public StudentVerificationResponse markPendingReview(
            Integer userId,
            Integer verificationId
    ) {
        VerificacionUsuario verification = requireOwnedVerification(
                userId,
                verificationId
        );
        verification.setEstadoVerificacion(REVIEW_PENDING);
        verification.setObservacion(null);
        verificationRepository.save(verification);
        UsuarioEstudiante student = studentRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No existe el perfil estudiantil."
                ));
        student.setEstadoPerfilEstudiante(PROFILE_PENDING);
        studentRepository.save(student);
        return toResponse(verification);
    }

    @Transactional(readOnly = true)
    public StudentVerificationResponse getCurrent(Integer userId) {
        VerificacionUsuario verification = verificationRepository
                .findFirstByIdUsuarioAndTipoVerificacionOrderByFechaSolicitudDesc(
                        userId,
                        VERIFICATION_TYPE
                )
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Aun no has enviado documentos para revision."
                ));
        return toResponse(verification);
    }

    @Transactional(readOnly = true)
    public List<StudentVerificationResponse> listPending() {
        return verificationRepository
                .findByTipoVerificacionAndEstadoVerificacionOrderByFechaSolicitudAsc(
                        VERIFICATION_TYPE,
                        REVIEW_PENDING
                )
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public StudentVerificationResponse approve(Integer userId, String observation) {
        VerificacionUsuario verification = requireReviewPending(userId);
        verification.setEstadoVerificacion(APPROVED);
        verification.setFechaVerificacion(LocalDateTime.now());
        verification.setUltimaRevalidacion(LocalDateTime.now());
        verification.setObservacion(cleanObservation(observation));

        UsuarioEstudiante student = studentRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No existe el perfil estudiantil."
                ));
        student.setEstadoPerfilEstudiante(PROFILE_ACTIVE);
        studentRepository.save(student);
        if (roleRepository.assignRole(userId, "ESTUDIANTE") == 0) {
            throw new IllegalStateException("No se encontro el rol ESTUDIANTE.");
        }
        return toResponse(verificationRepository.save(verification));
    }

    @Transactional
    public StudentVerificationResponse reject(Integer userId, String observation) {
        VerificacionUsuario verification = requireReviewPending(userId);
        verification.setEstadoVerificacion(REJECTED);
        verification.setObservacion(cleanObservation(observation));
        UsuarioEstudiante student = studentRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No existe el perfil estudiantil."
                ));
        student.setEstadoPerfilEstudiante(PROFILE_REJECTED);
        studentRepository.save(student);
        return toResponse(verificationRepository.save(verification));
    }

    private VerificacionUsuario createVerification(Integer userId) {
        Usuario user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "La cuenta indicada no existe."
                ));
        VerificacionUsuario verification = new VerificacionUsuario();
        verification.setIdUsuario(userId);
        verification.setCorreoInstitucional(user.getCorreo());
        verification.setCodigoVerificacion(null);
        verification.setEstadoVerificacion(DOCUMENT_PENDING);
        verification.setFechaSolicitud(LocalDateTime.now());
        verification.setTipoVerificacion(VERIFICATION_TYPE);
        return verificationRepository.save(verification);
    }

    private VerificacionUsuario requireOwnedVerification(
            Integer userId,
            Integer verificationId
    ) {
        VerificacionUsuario verification = verificationRepository.findById(verificationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "La verificacion indicada no existe."
                ));
        if (!verification.getIdUsuario().equals(userId)
                || !VERIFICATION_TYPE.equals(verification.getTipoVerificacion())) {
            throw new ResourceNotFoundException(
                    "La verificacion indicada no existe."
            );
        }
        return verification;
    }

    private VerificacionUsuario requireReviewPending(Integer userId) {
        return verificationRepository
                .findFirstByIdUsuarioAndTipoVerificacionOrderByFechaSolicitudDesc(
                        userId,
                        VERIFICATION_TYPE
                )
                .filter(item -> REVIEW_PENDING.equals(item.getEstadoVerificacion()))
                .orElseThrow(() -> new ResourceConflictException(
                        "La solicitud no esta pendiente de revision."
                ));
    }

    private StudentVerificationResponse toResponse(VerificacionUsuario verification) {
        return StudentVerificationResponse.fromEntity(
                verification,
                attachmentService.listVerificationAttachments(
                        verification.getIdVerificacion()
                )
        );
    }

    private String cleanObservation(String observation) {
        return observation == null || observation.isBlank()
                ? null
                : observation.trim();
    }
}
