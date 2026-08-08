package com.t4kash.app.ui.model

data class ConversationDto(
    val idConversacion: Int,
    val idTarea: Int,
    val idTrabajo: Int?,
    val tituloTarea: String,
    val idParticipante: Int,
    val nombreParticipante: String,
    val nombreUsuarioParticipante: String?,
    val estadoConversacion: String,
    val ultimoMensaje: String?,
    val fechaUltimoMensaje: String?,
    val mensajesNoLeidos: Long
)

data class MessageDto(
    val idMensaje: Int,
    val idConversacion: Int,
    val idUsuarioEmisor: Int,
    val nombreEmisor: String,
    val nombreUsuarioEmisor: String?,
    val contenido: String,
    val fechaEnvio: String,
    val leido: Boolean,
    val fechaLectura: String?,
    val propio: Boolean
)

data class NotificationDto(
    val idNotificacion: Int,
    val titulo: String,
    val mensaje: String,
    val leida: Boolean,
    val fechaCreacion: String
)

data class CreateMessageRequest(
    val contenido: String
)

data class CommunicationOverview(
    val conversations: List<ConversationDto>,
    val notifications: List<NotificationDto>
)
