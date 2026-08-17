package com.t4kash.app.ui.model

data class NetworkPostDto(
    val idPublicacion: Int,
    val autor: PublicIdentityDto,
    val idPublicacionOrigen: Int?,
    val contenido: String?,
    val tipoPublicacion: String,
    val visibilidad: String,
    val permiteComentarios: Boolean,
    val fechaPublicacion: String,
    val fechaEdicion: String?,
    val estadoPublicacion: String,
    val reacciones: Map<String, Long> = emptyMap(),
    val totalReacciones: Long,
    val totalComentarios: Long,
    val totalCompartidas: Long,
    val miReaccion: String?,
    val guardada: Boolean,
    val propia: Boolean
)

data class NetworkCommentDto(
    val idComentario: Int,
    val idPublicacion: Int,
    val idComentarioPadre: Int?,
    val autor: PublicIdentityDto,
    val contenido: String,
    val fechaComentario: String,
    val fechaEdicion: String?,
    val propio: Boolean
)

data class CreateNetworkPostRequest(
    val contenido: String?,
    val tipoPublicacion: String,
    val visibilidad: String,
    val permiteComentarios: Boolean,
    val idPublicacionOrigen: Int? = null
)

data class UpdateNetworkPostRequest(
    val contenido: String?,
    val tipoPublicacion: String,
    val visibilidad: String,
    val permiteComentarios: Boolean
)

data class NetworkReactionRequest(
    val tipoReaccion: String
)

data class CreateNetworkCommentRequest(
    val contenido: String,
    val idComentarioPadre: Int? = null
)

data class UpdateNetworkCommentRequest(
    val contenido: String
)

enum class NetworkFeedScope(
    val apiValue: String,
    val label: String
) {
    FOR_YOU("PARA_TI", "Para ti"),
    CONNECTIONS("CONEXIONES", "Conexiones"),
    UNIVERSITY("UNIVERSIDAD", "Universidad")
}

