package com.t4kash.api.marketplace.dto;

import com.t4kash.api.marketplace.entity.TrabajoAsignado;

import java.time.LocalDateTime;

public record JobResponse(
        Integer idTrabajo,
        Integer idTarea,
        Integer idEstudiante,
        LocalDateTime fechaInicio,
        LocalDateTime fechaEntregaEsperada,
        String estadoTrabajo
) {
    public static JobResponse fromEntity(TrabajoAsignado trabajo) {
        return new JobResponse(
                trabajo.getIdTrabajo(),
                trabajo.getIdTarea(),
                trabajo.getIdEstudiante(),
                trabajo.getFechaInicio(),
                trabajo.getFechaEntregaEsperada(),
                trabajo.getEstadoTrabajo()
        );
    }
}
