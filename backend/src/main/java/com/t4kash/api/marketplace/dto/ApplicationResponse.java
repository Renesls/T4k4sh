package com.t4kash.api.marketplace.dto;

import com.t4kash.api.identity.dto.PublicIdentityResponse;
import com.t4kash.api.marketplace.entity.Postulacion;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ApplicationResponse(
        Integer idPostulacion,
        Integer idTarea,
        Integer idEstudiante,
        String mensaje,
        BigDecimal precioPropuesto,
        LocalDateTime fechaPostulacion,
        String estadoPostulacion,
        Integer numeroIntento,
        PublicIdentityResponse estudiante
) {
    public static ApplicationResponse fromEntity(Postulacion postulacion) {
        return new ApplicationResponse(
                postulacion.getIdPostulacion(),
                postulacion.getIdTarea(),
                postulacion.getIdEstudiante(),
                postulacion.getMensaje(),
                postulacion.getPrecioPropuesto(),
                postulacion.getFechaPostulacion(),
                postulacion.getEstadoPostulacion(),
                postulacion.getNumeroIntento(),
                null
        );
    }

    public ApplicationResponse withStudent(PublicIdentityResponse publicStudent) {
        return new ApplicationResponse(
                idPostulacion, idTarea, idEstudiante, mensaje, precioPropuesto,
                fechaPostulacion, estadoPostulacion, numeroIntento, publicStudent
        );
    }
}
