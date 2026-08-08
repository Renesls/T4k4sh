package com.t4kash.api.marketplace.dto;

import com.t4kash.api.identity.dto.PublicIdentityResponse;
import com.t4kash.api.marketplace.entity.TrabajoAsignado;

import java.time.LocalDateTime;

public record JobResponse(
        Integer idTrabajo,
        Integer idTarea,
        Integer idEstudiante,
        LocalDateTime fechaInicio,
        LocalDateTime fechaEntregaEsperada,
        String estadoTrabajo,
        PublicIdentityResponse estudiante
) {
    public static JobResponse fromEntity(TrabajoAsignado trabajo) {
        return new JobResponse(
                trabajo.getIdTrabajo(),
                trabajo.getIdTarea(),
                trabajo.getIdEstudiante(),
                trabajo.getFechaInicio(),
                trabajo.getFechaEntregaEsperada(),
                trabajo.getEstadoTrabajo(),
                null
        );
    }

    public JobResponse withStudent(PublicIdentityResponse publicStudent) {
        return new JobResponse(
                idTrabajo, idTarea, idEstudiante, fechaInicio,
                fechaEntregaEsperada, estadoTrabajo, publicStudent
        );
    }
}
