package com.t4kash.api.identity.dto;

import com.t4kash.api.identity.entity.VerificacionUsuario;
import com.t4kash.api.marketplace.dto.AttachmentResponse;

import java.time.LocalDateTime;
import java.util.List;

public record StudentVerificationResponse(
        Integer idVerificacion,
        Integer idUsuario,
        String correo,
        String estado,
        String observacion,
        LocalDateTime fechaSolicitud,
        List<AttachmentResponse> archivos
) {
    public static StudentVerificationResponse fromEntity(
            VerificacionUsuario verification,
            List<AttachmentResponse> attachments
    ) {
        return new StudentVerificationResponse(
                verification.getIdVerificacion(),
                verification.getIdUsuario(),
                verification.getCorreoInstitucional(),
                verification.getEstadoVerificacion(),
                verification.getObservacion(),
                verification.getFechaSolicitud(),
                attachments
        );
    }
}
