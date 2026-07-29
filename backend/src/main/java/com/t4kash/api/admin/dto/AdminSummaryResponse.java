package com.t4kash.api.admin.dto;

public record AdminSummaryResponse(
        long usuarios,
        long verificacionesPendientes,
        long reportesPendientes,
        long publicacionesActivas,
        long trabajosAsignados
) {
}
