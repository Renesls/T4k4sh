package com.t4kash.api.moderation.dto;

import com.t4kash.api.moderation.entity.Reporte;

import java.time.LocalDateTime;

public record ReportResponse(
        Integer idReporte,
        Integer idUsuarioReporta,
        String correoReporta,
        Integer idUsuarioReportado,
        String correoReportado,
        Integer idTarea,
        String tituloTarea,
        String motivo,
        String descripcion,
        String estadoReporte,
        LocalDateTime fechaReporte,
        String tipoReporte,
        String categoriaReporte
) {
    public static ReportResponse fromEntity(
            Reporte report,
            String reporterEmail,
            String reportedEmail,
            String taskTitle
    ) {
        return new ReportResponse(
                report.getIdReporte(),
                report.getIdUsuarioReporta(),
                reporterEmail,
                report.getIdUsuarioReportado(),
                reportedEmail,
                report.getIdTarea(),
                taskTitle,
                report.getMotivo(),
                report.getDescripcion(),
                report.getEstadoReporte(),
                report.getFechaReporte(),
                report.getTipoReporte(),
                report.getCategoriaReporte()
        );
    }
}
