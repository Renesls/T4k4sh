package com.t4kash.app.ui.model

data class CategoryDto(
    val idCategoria: Int,
    val nombreCategoria: String,
    val descripcion: String?,
    val estado: Boolean
)

data class TaskDto(
    val idTarea: Int,
    val titulo: String,
    val descripcion: String,
    val presupuesto: Double,
    val fechaPublicacion: String,
    val fechaLimitePostulacion: String?,
    val fechaLimite: String?,
    val estadoTarea: String,
    val idCategoria: Int,
    val idCliente: Int,
    val tipoOportunidad: String,
    val modalidad: String?,
    val visibilidad: String,
    val direccionReferencia: String?,
    val latitud: Double?,
    val longitud: Double?
)

data class CreateTaskRequest(
    val titulo: String,
    val descripcion: String,
    val presupuesto: Double,
    val fechaLimitePostulacion: String? = null,
    val fechaLimite: String? = null,
    val idCategoria: Int,
    val idCliente: Int,
    val tipoOportunidad: String = "TAREA",
    val modalidad: String,
    val visibilidad: String = "PUBLICA",
    val direccionReferencia: String?,
    val latitud: Double?,
    val longitud: Double?
)

data class CreateApplicationRequest(
    val idEstudiante: Int,
    val mensaje: String?,
    val precioPropuesto: Double?
)

data class ApplicationDto(
    val idPostulacion: Int,
    val idTarea: Int,
    val idEstudiante: Int,
    val mensaje: String?,
    val precioPropuesto: Double?,
    val fechaPostulacion: String,
    val estadoPostulacion: String
)

data class JobDto(
    val idTrabajo: Int,
    val idTarea: Int,
    val idEstudiante: Int,
    val fechaInicio: String,
    val fechaEntregaEsperada: String?,
    val estadoTrabajo: String
)

data class MarketplaceHomeData(
    val categories: List<CategoryDto>,
    val tasks: List<TaskDto>
)
