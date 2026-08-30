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
    val longitud: Double?,
    val cliente: PublicIdentityDto?
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

data class QuickTaskDto(
    val tarea: TaskDto,
    val distanciaKm: Double,
    val segundosRestantes: Long
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
    val numeroIntento: Int,
    val estudiante: PublicIdentityDto?
)

data class AcceptApplicationRequest(
    val metodoPago: String
)

data class PaymentDto(
    val idPago: Int,
    val idTrabajo: Int,
    val idCliente: Int,
    val idEstudiante: Int,
    val proveedorPago: String,
    val entornoPago: String,
    val metodoPago: String,
    val monedaCobro: String,
    val montoEstudiante: Double,
    val porcentajeComisionPlataforma: Double,
    val comisionPlataforma: Double,
    val comisionProcesador: Double,
    val impuestoProcesador: Double,
    val montoTotalCliente: Double,
    val estadoPago: String,
    val referenciaComercio: String?,
    val fechaCreacion: String,
    val fechaActualizacion: String,
    val fechaExpiracion: String?,
    val fechaConfirmacion: String?,
    val fechaLiberacion: String?,
    val puedePagar: Boolean
)

data class CheckoutDto(
    val idPago: Int,
    val checkoutUrl: String,
    val estadoPago: String
)

data class WalletMovementDto(
    val idTransaccion: Long,
    val idPago: Int,
    val tipoMovimiento: String,
    val saldoAfectado: String,
    val monto: Double,
    val moneda: String,
    val estadoMovimiento: String,
    val proveedorPago: String,
    val descripcion: String?,
    val fechaRegistro: String
)

data class WalletDto(
    val moneda: String,
    val balanceDisponible: Double,
    val fondosRetenidos: Double,
    val totalGanado: Double,
    val pagos: List<PaymentDto>,
    val movimientos: List<WalletMovementDto>
)

data class JobDto(
    val idTrabajo: Int,
    val idTarea: Int,
    val idEstudiante: Int,
    val fechaInicio: String,
    val fechaEntregaEsperada: String?,
    val estadoTrabajo: String,
    val estudiante: PublicIdentityDto?,
    val pago: PaymentDto? = null
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

data class CreateRatingRequest(
    val puntuacion: Int,
    val comentario: String?
)

data class RatingDto(
    val idCalificacion: Int,
    val idTrabajo: Int,
    val idCalificador: Int,
    val idCalificado: Int,
    val puntuacion: Int,
    val comentario: String?,
    val fechaCalificacion: String,
    val calificador: PublicIdentityDto?
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
