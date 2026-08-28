package com.t4kash.api.identity.dto;

import com.t4kash.api.identity.entity.VerificacionIdentidad;

import java.time.LocalDateTime;

public record IdentityVerificationStatusResponse(
        Integer idVerificacion,
        String estado,
        String estadoProveedor,
        boolean verificada,
        boolean operacionesProtegidasHabilitadas,
        String mensaje,
        LocalDateTime fechaInicio,
        LocalDateTime fechaActualizacion,
        LocalDateTime fechaDecision,
        LocalDateTime fechaExpiracion
) {
    public static IdentityVerificationStatusResponse notStarted() {
        return new IdentityVerificationStatusResponse(
                null,
                "NO_INICIADA",
                null,
                false,
                false,
                "Verifica tu identidad para aceptar trabajos y utilizar Wallet.",
                null,
                null,
                null,
                null
        );
    }

    public static IdentityVerificationStatusResponse fromEntity(
            VerificacionIdentidad verification,
            boolean approved
    ) {
        String state = approved
                ? "APROBADA"
                : "APROBADA".equals(verification.getEstadoVerificacion())
                    ? "VENCIDA"
                    : verification.getEstadoVerificacion();
        return new IdentityVerificationStatusResponse(
                verification.getIdVerificacionIdentidad(),
                state,
                verification.getEstadoProveedor(),
                approved,
                approved,
                messageFor(state),
                verification.getFechaInicio(),
                verification.getFechaActualizacion(),
                verification.getFechaDecision(),
                verification.getFechaExpiracion()
        );
    }

    private static String messageFor(String state) {
        return switch (state) {
            case "PENDIENTE" -> "La verificacion esta lista para comenzar en Didit.";
            case "EN_PROCESO" -> "Continua los pasos pendientes en Didit.";
            case "EN_REVISION" -> "Didit esta revisando la informacion enviada.";
            case "APROBADA" -> "Tu identidad esta verificada y las operaciones protegidas estan habilitadas.";
            case "RECHAZADA" -> "La identidad no pudo ser aprobada. Puedes iniciar un nuevo intento.";
            case "EXPIRADA", "VENCIDA" -> "La verificacion vencio. Inicia un nuevo intento para continuar.";
            case "ABANDONADA" -> "El proceso no se completo. Puedes retomarlo cuando quieras.";
            case "REQUIERE_ACCION" -> "Didit necesita que completes un paso adicional.";
            case "CANCELADA" -> "La verificacion fue cancelada. Puedes iniciar una nueva.";
            default -> "Consulta el estado antes de realizar operaciones protegidas.";
        };
    }
}
