package com.t4kash.api.marketplace.dto;

import com.t4kash.api.identity.dto.PublicIdentityResponse;
import com.t4kash.api.finance.dto.PaymentResponse;
import com.t4kash.api.marketplace.entity.TrabajoAsignado;

import java.time.LocalDateTime;

public record JobResponse(
        Integer idTrabajo,
        Integer idTarea,
        Integer idEstudiante,
        LocalDateTime fechaInicio,
        LocalDateTime fechaEntregaEsperada,
        String estadoTrabajo,
        PublicIdentityResponse estudiante,
        PaymentResponse pago
) {
    public static JobResponse fromEntity(TrabajoAsignado trabajo) {
        return new JobResponse(
                trabajo.getIdTrabajo(),
                trabajo.getIdTarea(),
                trabajo.getIdEstudiante(),
                trabajo.getFechaInicio(),
                trabajo.getFechaEntregaEsperada(),
                trabajo.getEstadoTrabajo(),
                null,
                null
        );
    }

    public JobResponse withStudent(PublicIdentityResponse publicStudent) {
        return new JobResponse(
                idTrabajo, idTarea, idEstudiante, fechaInicio,
                fechaEntregaEsperada, estadoTrabajo, publicStudent, pago
        );
    }

    public JobResponse withPayment(PaymentResponse payment) {
        return new JobResponse(
                idTrabajo, idTarea, idEstudiante, fechaInicio,
                fechaEntregaEsperada, estadoTrabajo, estudiante, payment
        );
    }
}
