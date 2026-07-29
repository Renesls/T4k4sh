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
    val tipoOportunidad: String = "TAREA",
    val modalidad: String,
    val visibilidad: String = "PUBLICA",
    val direccionReferencia: String?,
    val latitud: Double?,
    val longitud: Double?
)

data class CreateApplicationRequest(
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
    val estadoPostulacion: String,
    val numeroIntento: Int
)

data class JobDto(
    val idTrabajo: Int,
    val idTarea: Int,
    val idEstudiante: Int,
    val fechaInicio: String,
    val fechaEntregaEsperada: String?,
    val estadoTrabajo: String
)

data class CreateDeliveryRequest(
    val descripcionEntrega: String
)

data class DeliveryDto(
    val idEntrega: Int,
    val idTrabajo: Int,
    val descripcionEntrega: String,
    val fechaEntrega: String,
    val estadoEntrega: String
)

data class PendingAttachment(
    val name: String,
    val mimeType: String,
    val content: ByteArray
)

data class AttachmentDto(
    val idArchivo: Int,
    val idTarea: Int?,
    val idEntrega: Int?,
    val idVerificacion: Int?,
    val idUsuarioSube: Int,
    val nombreOriginal: String,
    val tipoMime: String,
    val extension: String?,
    val tamanoBytes: Long,
    val fechaSubida: String,
    val estadoArchivo: String,
    val rutaDescarga: String
)

data class MarketplaceHomeData(
    val categories: List<CategoryDto>,
    val tasks: List<TaskDto>
)

data class AdminSummaryDto(
    val usuarios: Long,
    val verificacionesPendientes: Long,
    val reportesPendientes: Long,
    val publicacionesActivas: Long,
    val trabajosAsignados: Long
)

data class StudentVerificationDto(
    val idVerificacion: Int,
    val idUsuario: Int,
    val correo: String,
    val estado: String,
    val observacion: String?,
    val fechaSolicitud: String,
    val archivos: List<AttachmentDto>
)

data class ReviewStudentVerificationRequest(
    val observacion: String?
)

data class AdminDashboardData(
    val summary: AdminSummaryDto,
    val verifications: List<StudentVerificationDto>,
    val reports: List<ReportDto>,
    val tasks: List<TaskDto>
)

data class CreateTaskReportRequest(
    val categoriaReporte: String,
    val descripcion: String?
)

data class ReviewReportRequest(
    val estadoReporte: String,
    val observacion: String?,
    val retirarPublicacion: Boolean
)

data class ReportDto(
    val idReporte: Int,
    val idUsuarioReporta: Int,
    val correoReporta: String?,
    val idUsuarioReportado: Int?,
    val correoReportado: String?,
    val idTarea: Int?,
    val tituloTarea: String?,
    val motivo: String,
    val descripcion: String?,
    val estadoReporte: String,
    val fechaReporte: String,
    val tipoReporte: String,
    val categoriaReporte: String
)
