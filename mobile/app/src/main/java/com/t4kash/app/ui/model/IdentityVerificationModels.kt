package com.t4kash.app.ui.model

data class IdentityVerificationStatusDto(
    val idVerificacion: Int?,
    val estado: String,
    val estadoProveedor: String?,
    val verificada: Boolean,
    val operacionesProtegidasHabilitadas: Boolean,
    val mensaje: String,
    val fechaInicio: String?,
    val fechaActualizacion: String?,
    val fechaDecision: String?,
    val fechaExpiracion: String?
)

data class IdentityVerificationSessionDto(
    val idSesionProveedor: String,
    val urlVerificacion: String?,
    val estado: String
)
