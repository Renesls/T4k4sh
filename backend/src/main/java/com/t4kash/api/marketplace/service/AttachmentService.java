package com.t4kash.api.marketplace.service;

import com.t4kash.api.exception.ResourceNotFoundException;
import com.t4kash.api.marketplace.dto.AttachmentResponse;
import com.t4kash.api.marketplace.entity.ArchivoAdjunto;
import com.t4kash.api.marketplace.repository.ArchivoAdjuntoRepository;
import com.t4kash.api.marketplace.repository.EntregaRepository;
import com.t4kash.api.marketplace.repository.TareaRepository;
import com.t4kash.api.marketplace.repository.TrabajoAsignadoRepository;
import com.t4kash.api.marketplace.repository.UsuarioRepository;
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
    private final UsuarioRepository usuarioRepository;
    private final ObjectStorage objectStorage;

    public AttachmentService(
            ArchivoAdjuntoRepository archivoRepository,
            TareaRepository tareaRepository,
            EntregaRepository entregaRepository,
            TrabajoAsignadoRepository trabajoRepository,
            UsuarioRepository usuarioRepository,
            ObjectStorage objectStorage
    ) {
        this.archivoRepository = archivoRepository;
        this.tareaRepository = tareaRepository;
        this.entregaRepository = entregaRepository;
        this.trabajoRepository = trabajoRepository;
        this.usuarioRepository = usuarioRepository;
        this.objectStorage = objectStorage;
    }

    @Transactional
    public AttachmentResponse attachToTask(
            Integer taskId,
            Integer userId,
            MultipartFile file
    ) {
        if (!tareaRepository.existsById(taskId)) {
            throw new ResourceNotFoundException("La tarea indicada no existe.");
        }
        return store(file, userId, taskId, null, "tasks/" + taskId);
    }

    @Transactional
    public AttachmentResponse attachToDelivery(
            Integer deliveryId,
            Integer userId,
            MultipartFile file
    ) {
        if (!entregaRepository.existsById(deliveryId)) {
            throw new ResourceNotFoundException("La entrega indicada no existe.");
        }
        return store(file, userId, null, deliveryId, "deliveries/" + deliveryId);
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
    public List<AttachmentResponse> listDeliveryAttachments(Integer deliveryId) {
        if (!entregaRepository.existsById(deliveryId)) {
            throw new ResourceNotFoundException("La entrega indicada no existe.");
        }
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
    public List<AttachmentResponse> listJobAttachments(Integer jobId) {
        if (!trabajoRepository.existsById(jobId)) {
            throw new ResourceNotFoundException("El trabajo indicado no existe.");
        }
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
    public DownloadedAttachment download(Integer attachmentId) {
        ArchivoAdjunto attachment = archivoRepository.findById(attachmentId)
                .filter(file -> ACTIVE_STATUS.equals(file.getEstadoArchivo()))
                .orElseThrow(() -> new ResourceNotFoundException(
                        "El archivo adjunto indicado no existe."
                ));
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
            String folder
    ) {
        if (!usuarioRepository.existsById(userId)) {
            throw new ResourceNotFoundException("El usuario indicado no existe.");
        }
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

    private record ValidatedFile(
            String originalName,
            String safeName,
            String extension,
            String mimeType
    ) {
    }
}
