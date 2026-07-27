package com.t4kash.api.marketplace.dto;

import com.t4kash.api.marketplace.entity.ArchivoAdjunto;

import java.time.LocalDateTime;

public record AttachmentResponse(
        Integer idArchivo,
        Integer idTarea,
        Integer idEntrega,
        Integer idUsuarioSube,
        String nombreOriginal,
        String tipoMime,
        String extension,
        Long tamanoBytes,
        LocalDateTime fechaSubida,
        String estadoArchivo,
        String rutaDescarga
) {
    public static AttachmentResponse fromEntity(ArchivoAdjunto archivo) {
        return new AttachmentResponse(
                archivo.getIdArchivo(),
                archivo.getIdTarea(),
                archivo.getIdEntrega(),
                archivo.getIdUsuarioSube(),
                archivo.getNombreOriginal(),
                archivo.getTipoMime(),
                archivo.getExtension(),
                archivo.getTamanoBytes(),
                archivo.getFechaSubida(),
                archivo.getEstadoArchivo(),
                "attachments/" + archivo.getIdArchivo() + "/download"
        );
    }
}
