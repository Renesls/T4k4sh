package com.t4kash.api.marketplace.service;

import com.t4kash.api.exception.ForbiddenOperationException;
import com.t4kash.api.exception.ResourceNotFoundException;
import com.t4kash.api.marketplace.dto.AttachmentResponse;
import com.t4kash.api.marketplace.entity.ArchivoAdjunto;
import com.t4kash.api.marketplace.entity.Entrega;
import com.t4kash.api.marketplace.entity.Tarea;
import com.t4kash.api.marketplace.entity.TrabajoAsignado;
import com.t4kash.api.marketplace.repository.ArchivoAdjuntoRepository;
import com.t4kash.api.marketplace.repository.EntregaRepository;
import com.t4kash.api.marketplace.repository.TareaRepository;
import com.t4kash.api.marketplace.repository.TrabajoAsignadoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class AttachmentService {
    public static final long MAX_FILE_SIZE = 10L * 1024L * 1024L;
    private static final String ACTIVE_STATUS = "ACTIVO";
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "pdf", "png", "jpg", "jpeg", "webp", "txt", "doc", "docx", "zip"
    );
    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
            "application/pdf",
            "image/png",
            "image/jpeg",
            "image/webp",
            "text/plain",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/zip",
            "application/x-zip-compressed",
            "application/octet-stream"
    );
    private static final Map<String, String> MIME_BY_EXTENSION = Map.ofEntries(
            Map.entry("pdf", "application/pdf"),
            Map.entry("png", "image/png"),
            Map.entry("jpg", "image/jpeg"),
            Map.entry("jpeg", "image/jpeg"),
            Map.entry("webp", "image/webp"),
            Map.entry("txt", "text/plain"),
            Map.entry("doc", "application/msword"),
            Map.entry(
                    "docx",
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            ),
            Map.entry("zip", "application/zip")
    );

    private final ArchivoAdjuntoRepository archivoRepository;
    private final TareaRepository tareaRepository;
    private final EntregaRepository entregaRepository;
    private final TrabajoAsignadoRepository trabajoRepository;
    private final ObjectStorage objectStorage;

    public AttachmentService(
            ArchivoAdjuntoRepository archivoRepository,
            TareaRepository tareaRepository,
            EntregaRepository entregaRepository,
            TrabajoAsignadoRepository trabajoRepository,
            ObjectStorage objectStorage
    ) {
        this.archivoRepository = archivoRepository;
        this.tareaRepository = tareaRepository;
        this.entregaRepository = entregaRepository;
        this.trabajoRepository = trabajoRepository;
        this.objectStorage = objectStorage;
    }

    @Transactional
    public AttachmentResponse attachToTask(
            Integer taskId,
            Integer userId,
            MultipartFile file
    ) {
        Tarea tarea = findTask(taskId);
        requireTaskOwner(tarea, userId);
        return store(file, userId, taskId, null, null, "tasks/" + taskId);
    }

    @Transactional
    public AttachmentResponse attachToDelivery(
            Integer deliveryId,
            Integer userId,
            MultipartFile file
    ) {
        Entrega entrega = findDelivery(deliveryId);
        TrabajoAsignado trabajo = findJob(entrega.getIdTrabajo());
        requireAssignedStudent(trabajo, userId);
        return store(file, userId, null, deliveryId, null, "deliveries/" + deliveryId);
    }

    @Transactional
    public AttachmentResponse attachToVerification(
            Integer verificationId,
            Integer userId,
            MultipartFile file
    ) {
        return store(
                file,
                userId,
                null,
                null,
                verificationId,
                "student-verifications/" + verificationId
        );
    }

    @Transactional(readOnly = true)
    public List<AttachmentResponse> listTaskAttachments(Integer taskId) {
        if (!tareaRepository.existsById(taskId)) {
            throw new ResourceNotFoundException("La tarea indicada no existe.");
        }
        return archivoRepository
                .findByIdTareaAndEstadoArchivoOrderByFechaSubidaDesc(taskId, ACTIVE_STATUS)
                .stream()
                .map(AttachmentResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AttachmentResponse> listDeliveryAttachments(
            Integer deliveryId,
            Integer userId
    ) {
        Entrega entrega = findDelivery(deliveryId);
        requireJobParticipant(findJob(entrega.getIdTrabajo()), userId);
        return archivoRepository
                .findByIdEntregaAndEstadoArchivoOrderByFechaSubidaDesc(
                        deliveryId,
                        ACTIVE_STATUS
                )
                .stream()
                .map(AttachmentResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AttachmentResponse> listJobAttachments(Integer jobId, Integer userId) {
        TrabajoAsignado trabajo = findJob(jobId);
        requireJobParticipant(trabajo, userId);
        List<Integer> deliveryIds = entregaRepository
                .findByIdTrabajoOrderByFechaEntregaDesc(jobId)
                .stream()
                .map(delivery -> delivery.getIdEntrega())
                .toList();
        if (deliveryIds.isEmpty()) {
            return List.of();
        }
        return archivoRepository
                .findByIdEntregaInAndEstadoArchivoOrderByFechaSubidaDesc(
                        deliveryIds,
                        ACTIVE_STATUS
                )
                .stream()
                .map(AttachmentResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AttachmentResponse> listVerificationAttachments(Integer verificationId) {
        return archivoRepository
                .findByIdVerificacionAndEstadoArchivoOrderByFechaSubidaDesc(
                        verificationId,
                        ACTIVE_STATUS
                )
                .stream()
                .map(AttachmentResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public DownloadedAttachment download(Integer attachmentId, Integer userId) {
        return download(attachmentId, userId, false);
    }

    @Transactional(readOnly = true)
    public DownloadedAttachment download(
            Integer attachmentId,
            Integer userId,
            boolean administrator
    ) {
        ArchivoAdjunto attachment = archivoRepository.findById(attachmentId)
                .filter(file -> ACTIVE_STATUS.equals(file.getEstadoArchivo()))
                .orElseThrow(() -> new ResourceNotFoundException(
                        "El archivo adjunto indicado no existe."
                ));
        requireAttachmentAccess(attachment, userId, administrator);
        return new DownloadedAttachment(
                attachment.getNombreOriginal(),
                attachment.getTipoMime(),
                objectStorage.download(attachment.getRutaStorage())
        );
    }

    private AttachmentResponse store(
            MultipartFile file,
            Integer userId,
            Integer taskId,
            Integer deliveryId,
            Integer verificationId,
            String folder
    ) {
        ValidatedFile validated = validate(file);
        String objectPath = folder + "/" + UUID.randomUUID() + "-" + validated.safeName();
        byte[] content;
        try {
            content = file.getBytes();
        } catch (IOException ex) {
            throw new IllegalArgumentException("No se pudo leer el archivo seleccionado.");
        }

        objectStorage.upload(objectPath, validated.mimeType(), content);
        try {
            ArchivoAdjunto attachment = new ArchivoAdjunto();
            attachment.setIdTarea(taskId);
            attachment.setIdEntrega(deliveryId);
            attachment.setIdVerificacion(verificationId);
            attachment.setIdUsuarioSube(userId);
            attachment.setNombreOriginal(validated.originalName());
            attachment.setTipoMime(validated.mimeType());
            attachment.setExtension(validated.extension());
            attachment.setTamanoBytes(file.getSize());
            attachment.setBucketStorage(objectStorage.bucketName());
            attachment.setRutaStorage(objectPath);
            attachment.setFechaSubida(LocalDateTime.now());
            attachment.setEstadoArchivo(ACTIVE_STATUS);
            return AttachmentResponse.fromEntity(archivoRepository.saveAndFlush(attachment));
        } catch (RuntimeException ex) {
            objectStorage.delete(objectPath);
            throw ex;
        }
    }

    private ValidatedFile validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Selecciona un archivo con contenido.");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("El archivo no puede superar los 10 MB.");
        }

        String originalName = cleanOriginalName(file.getOriginalFilename());
        String extension = extensionOf(originalName);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException(
                    "Tipo de archivo no permitido. Usa PDF, imagen, TXT, Word o ZIP."
            );
        }

        String mimeType = normalizeMime(file.getContentType(), extension);
        if (!ALLOWED_MIME_TYPES.contains(mimeType)) {
            throw new IllegalArgumentException(
                    "El tipo MIME del archivo no esta permitido."
            );
        }
        return new ValidatedFile(
                originalName,
                sanitizeFileName(originalName),
                extension,
                mimeType
        );
    }

    private String cleanOriginalName(String value) {
        String name = value == null ? "" : value.replace('\\', '/');
        name = name.substring(name.lastIndexOf('/') + 1).trim();
        if (name.isBlank()) {
            throw new IllegalArgumentException("El archivo debe tener un nombre valido.");
        }
        return name.length() > 255 ? name.substring(name.length() - 255) : name;
    }

    private String sanitizeFileName(String value) {
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        String safe = normalized.replaceAll("[^A-Za-z0-9._-]", "_")
                .replaceAll("_+", "_");
        return safe.isBlank() ? "archivo" : safe;
    }

    private String extensionOf(String fileName) {
        int dot = fileName.lastIndexOf('.');
        if (dot < 0 || dot == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private String normalizeMime(String contentType, String extension) {
        String mime = contentType == null ? "" : contentType;
        int separator = mime.indexOf(';');
        if (separator >= 0) {
            mime = mime.substring(0, separator);
        }
        mime = mime.trim().toLowerCase(Locale.ROOT);
        if (mime.isBlank() || "application/octet-stream".equals(mime)) {
            return MIME_BY_EXTENSION.getOrDefault(extension, "application/octet-stream");
        }
        return mime;
    }

    private void requireAttachmentAccess(
            ArchivoAdjunto attachment,
            Integer userId,
            boolean administrator
    ) {
        if (attachment.getIdTarea() != null) {
            findTask(attachment.getIdTarea());
            return;
        }
        if (attachment.getIdEntrega() != null) {
            Entrega entrega = findDelivery(attachment.getIdEntrega());
            requireJobParticipant(findJob(entrega.getIdTrabajo()), userId);
            return;
        }
        if (attachment.getIdVerificacion() != null) {
            if (administrator || attachment.getIdUsuarioSube().equals(userId)) {
                return;
            }
            throw new ForbiddenOperationException(
                    "Solo el estudiante o un administrador pueden consultar este archivo."
            );
        }
        throw new ForbiddenOperationException(
                "El archivo no esta asociado a un recurso accesible."
        );
    }

    private void requireTaskOwner(Tarea tarea, Integer userId) {
        if (!tarea.getIdCliente().equals(userId)) {
            throw new ForbiddenOperationException(
                    "Solo el propietario de la tarea puede adjuntar archivos."
            );
        }
    }

    private void requireAssignedStudent(TrabajoAsignado trabajo, Integer userId) {
        if (!trabajo.getIdEstudiante().equals(userId)) {
            throw new ForbiddenOperationException(
                    "Solo el estudiante asignado puede adjuntar archivos a la entrega."
            );
        }
    }

    private void requireJobParticipant(TrabajoAsignado trabajo, Integer userId) {
        Tarea tarea = findTask(trabajo.getIdTarea());
        boolean isParticipant =
                trabajo.getIdEstudiante().equals(userId) ||
                tarea.getIdCliente().equals(userId);
        if (!isParticipant) {
            throw new ForbiddenOperationException(
                    "Solo los participantes del trabajo pueden consultar sus archivos."
            );
        }
    }

    private Tarea findTask(Integer taskId) {
        return tareaRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "La tarea indicada no existe."
                ));
    }

    private Entrega findDelivery(Integer deliveryId) {
        return entregaRepository.findById(deliveryId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "La entrega indicada no existe."
                ));
    }

    private TrabajoAsignado findJob(Integer jobId) {
        return trabajoRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "El trabajo indicado no existe."
                ));
    }

    private record ValidatedFile(
            String originalName,
            String safeName,
            String extension,
            String mimeType
    ) {
    }
}
