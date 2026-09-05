package com.t4kash.app.ui.repository

import com.t4kash.app.ui.model.ApplicationDto
import com.t4kash.app.ui.model.AcceptApplicationRequest
import com.t4kash.app.ui.model.AdminDashboardData
import com.t4kash.app.ui.model.AttachmentDto
import com.t4kash.app.ui.model.CreateApplicationRequest
import com.t4kash.app.ui.model.CreateDeliveryCommentRequest
import com.t4kash.app.ui.model.CreateDeliveryRequest
import com.t4kash.app.ui.model.CreatePaymentDisputeRequest
import com.t4kash.app.ui.model.CreateTaskRequest
import com.t4kash.app.ui.model.CreateTaskReportRequest
import com.t4kash.app.ui.model.CreateRatingRequest
import com.t4kash.app.ui.model.DeliveryDto
import com.t4kash.app.ui.model.RatingDto
import com.t4kash.app.ui.model.JobDto
import com.t4kash.app.ui.model.CheckoutDto
import com.t4kash.app.ui.model.PaymentDto
import com.t4kash.app.ui.model.PaymentDisputeDto
import com.t4kash.app.ui.model.QuickTaskDto
import com.t4kash.app.ui.model.RequestDeliveryChangesRequest
import com.t4kash.app.ui.model.ResolvePaymentDisputeRequest
import com.t4kash.app.ui.model.WalletDto
import com.t4kash.app.ui.model.MarketplaceHomeData
import com.t4kash.app.ui.model.PendingAttachment
import com.t4kash.app.ui.model.ReviewStudentVerificationRequest
import com.t4kash.app.ui.model.ReviewReportRequest
import com.t4kash.app.ui.model.ReportDto
import com.t4kash.app.ui.model.StudentVerificationDto
import com.t4kash.app.ui.model.TaskDto
import com.t4kash.app.ui.service.ApiResult
import com.t4kash.app.ui.service.MarketplaceApiService
import com.t4kash.app.ui.service.RetrofitClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.json.JSONObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.HttpException
import java.io.File

class MarketplaceRepository(
    private val api: MarketplaceApiService = RetrofitClient.marketplaceApiService
) {
    suspend fun loadHomeData(): ApiResult<MarketplaceHomeData> {
        return try {
            coroutineScope {
                val categories = async { api.getCategories() }
                val tasks = async { api.getTasks() }
                ApiResult.Success(
                    MarketplaceHomeData(
                        categories = categories.await(),
                        tasks = tasks.await()
                    )
                )
            }
        } catch (e: Exception) {
            ApiResult.Error(e.apiMessage("No se pudo conectar con la API."))
        }
    }

    suspend fun createTask(request: CreateTaskRequest): ApiResult<TaskDto> {
        return try {
            ApiResult.Success(api.createTask(request))
        } catch (e: Exception) {
            ApiResult.Error(e.apiMessage("No se pudo publicar la oportunidad."))
        }
    }

    suspend fun updateTask(
        taskId: Int,
        request: CreateTaskRequest
    ): ApiResult<TaskDto> {
        return try {
            ApiResult.Success(api.updateTask(taskId, request))
        } catch (e: Exception) {
            ApiResult.Error(e.apiMessage("No se pudo actualizar la oportunidad."))
        }
    }

    suspend fun cancelTask(taskId: Int): ApiResult<TaskDto> {
        return try {
            ApiResult.Success(api.cancelTask(taskId))
        } catch (e: Exception) {
            ApiResult.Error(e.apiMessage("No se pudo cancelar la oportunidad."))
        }
    }

    suspend fun applyToTask(
        taskId: Int,
        request: CreateApplicationRequest
    ): ApiResult<ApplicationDto> {
        return try {
            ApiResult.Success(api.applyToTask(taskId, request))
        } catch (e: Exception) {
            ApiResult.Error(e.apiMessage("No se pudo enviar la postulacion."))
        }
    }

    suspend fun loadApplications(taskId: Int): ApiResult<List<ApplicationDto>> {
        return try {
            ApiResult.Success(api.getApplications(taskId))
        } catch (e: Exception) {
            ApiResult.Error(e.apiMessage("No se pudieron cargar las postulaciones."))
        }
    }

    suspend fun loadMyApplications(): ApiResult<List<ApplicationDto>> {
        return try {
            ApiResult.Success(api.getMyApplications())
        } catch (e: Exception) {
            ApiResult.Error(e.apiMessage("No se pudo cargar tu historial de postulaciones."))
        }
    }

    suspend fun acceptApplication(
        applicationId: Int,
        paymentMethod: String
    ): ApiResult<JobDto> {
        return try {
            ApiResult.Success(
                api.acceptApplication(
                    applicationId,
                    AcceptApplicationRequest(paymentMethod)
                )
            )
        } catch (e: Exception) {
            ApiResult.Error(e.apiMessage("No se pudo aceptar la postulacion."))
        }
    }

    suspend fun rejectApplication(applicationId: Int): ApiResult<ApplicationDto> {
        return try {
            ApiResult.Success(api.rejectApplication(applicationId))
        } catch (e: Exception) {
            ApiResult.Error(e.apiMessage("No se pudo rechazar la postulacion."))
        }
    }

    suspend fun loadJobs(): ApiResult<List<JobDto>> {
        return try {
            ApiResult.Success(api.getJobs())
        } catch (e: Exception) {
            ApiResult.Error(e.apiMessage("No se pudieron cargar los trabajos asignados."))
        }
    }

    suspend fun loadDeliveries(jobId: Int): ApiResult<List<DeliveryDto>> {
        return try {
            ApiResult.Success(api.getDeliveries(jobId))
        } catch (e: Exception) {
            ApiResult.Error(e.apiMessage("No se pudieron cargar las entregas."))
        }
    }

    suspend fun createDelivery(
        jobId: Int,
        request: CreateDeliveryRequest
    ): ApiResult<DeliveryDto> {
        return try {
            ApiResult.Success(api.createDelivery(jobId, request))
        } catch (e: Exception) {
            ApiResult.Error(e.apiMessage("No se pudo enviar la entrega."))
        }
    }

    suspend fun approveDelivery(deliveryId: Int): ApiResult<DeliveryDto> {
        return try {
            ApiResult.Success(api.approveDelivery(deliveryId))
        } catch (e: Exception) {
            ApiResult.Error(e.apiMessage("No se pudo aprobar la entrega."))
        }
    }

    suspend fun loadRatings(jobId: Int): ApiResult<List<RatingDto>> {
        return try {
            ApiResult.Success(api.getRatings(jobId))
        } catch (e: Exception) {
            ApiResult.Error(e.apiMessage("No se pudieron cargar las calificaciones."))
        }
    }

    suspend fun createRating(
        jobId: Int,
        request: CreateRatingRequest
    ): ApiResult<RatingDto> {
        return try {
            ApiResult.Success(api.createRating(jobId, request))
        } catch (e: Exception) {
            ApiResult.Error(e.apiMessage("No se pudo enviar la calificacion."))
        }
    }

    suspend fun requestDeliveryChanges(
        deliveryId: Int,
        request: RequestDeliveryChangesRequest
    ): ApiResult<DeliveryDto> {
        return try {
            ApiResult.Success(api.requestDeliveryChanges(deliveryId, request))
        } catch (e: Exception) {
            ApiResult.Error(e.apiMessage("No se pudieron solicitar los cambios."))
        }
    }

    suspend fun commentDelivery(
        deliveryId: Int,
        request: CreateDeliveryCommentRequest
    ): ApiResult<DeliveryDto> {
        return try {
            ApiResult.Success(api.commentDelivery(deliveryId, request))
        } catch (e: Exception) {
            ApiResult.Error(e.apiMessage("No se pudo registrar el comentario."))
        }
    }

    suspend fun loadNearbyQuickTasks(
        latitude: Double,
        longitude: Double,
        radiusKm: Double
    ): ApiResult<List<QuickTaskDto>> {
        return try {
            ApiResult.Success(
                api.getNearbyQuickTasks(latitude, longitude, radiusKm)
            )
        } catch (e: Exception) {
            ApiResult.Error(e.apiMessage("No se pudieron buscar tareas rapidas."))
        }
    }

    suspend fun claimQuickTask(taskId: Int): ApiResult<JobDto> {
        return try {
            ApiResult.Success(api.claimQuickTask(taskId))
        } catch (e: Exception) {
            ApiResult.Error(e.apiMessage("No se pudo tomar la tarea rapida."))
        }
    }

    suspend fun loadWallet(): ApiResult<WalletDto> {
        return try {
            ApiResult.Success(api.getWallet())
        } catch (e: Exception) {
            ApiResult.Error(e.apiMessage("No se pudo cargar tu Wallet."))
        }
    }

    suspend fun createPaymentCheckout(jobId: Int): ApiResult<CheckoutDto> {
        return try {
            ApiResult.Success(api.createPaymentCheckout(jobId))
        } catch (e: Exception) {
            ApiResult.Error(e.apiMessage("No se pudo abrir Pagadito Sandbox."))
        }
    }

    suspend fun confirmCashReceipt(jobId: Int): ApiResult<PaymentDto> {
        return try {
            ApiResult.Success(api.confirmCashReceipt(jobId))
        } catch (e: Exception) {
            ApiResult.Error(e.apiMessage("No se pudo confirmar el pago en efectivo."))
        }
    }

    suspend fun refreshPayment(paymentId: Int): ApiResult<PaymentDto> {
        return try {
            ApiResult.Success(api.refreshPayment(paymentId))
        } catch (e: Exception) {
            ApiResult.Error(e.apiMessage("No se pudo actualizar el pago."))
        }
    }

    suspend fun openPaymentDispute(
        paymentId: Int,
        reason: String,
        description: String,
        requestedSolution: String
    ): ApiResult<PaymentDisputeDto> {
        return try {
            ApiResult.Success(
                api.openPaymentDispute(
                    paymentId,
                    CreatePaymentDisputeRequest(reason, description, requestedSolution)
                )
            )
        } catch (e: Exception) {
            ApiResult.Error(e.apiMessage("No se pudo abrir la disputa."))
        }
    }

    suspend fun loadTaskAttachments(taskId: Int): ApiResult<List<AttachmentDto>> {
        return try {
            ApiResult.Success(api.getTaskAttachments(taskId))
        } catch (e: Exception) {
            ApiResult.Error(e.apiMessage("No se pudieron cargar los archivos de la tarea."))
        }
    }

    suspend fun loadJobAttachments(jobId: Int): ApiResult<List<AttachmentDto>> {
        return try {
            ApiResult.Success(api.getJobAttachments(jobId))
        } catch (e: Exception) {
            ApiResult.Error(e.apiMessage("No se pudieron cargar los archivos del trabajo."))
        }
    }

    suspend fun uploadTaskAttachment(
        taskId: Int,
        attachment: PendingAttachment
    ): ApiResult<AttachmentDto> {
        return uploadAttachment(attachment) { file ->
            api.uploadTaskAttachment(taskId, file)
        }
    }

    suspend fun uploadDeliveryAttachment(
        deliveryId: Int,
        attachment: PendingAttachment
    ): ApiResult<AttachmentDto> {
        return uploadAttachment(attachment) { file ->
            api.uploadDeliveryAttachment(deliveryId, file)
        }
    }

    suspend fun uploadStudentVerificationAttachment(
        attachment: PendingAttachment
    ): ApiResult<AttachmentDto> {
        return uploadAttachment(attachment) { file ->
            api.uploadStudentVerificationAttachment(file)
        }
    }

    suspend fun createTaskReport(
        taskId: Int,
        category: String,
        description: String?
    ): ApiResult<ReportDto> {
        return try {
            ApiResult.Success(
                api.createTaskReport(
                    taskId,
                    CreateTaskReportRequest(category, description)
                )
            )
        } catch (e: Exception) {
            ApiResult.Error(e.apiMessage("No se pudo enviar el reporte."))
        }
    }

    suspend fun loadAdminDashboard(): ApiResult<AdminDashboardData> {
        return try {
            coroutineScope {
                val summary = async { api.getAdminSummary() }
                val verifications = async { api.getPendingStudentVerifications() }
                val reports = async { api.getAdminReports() }
                val tasks = async { api.getAdminTasks() }
                val paymentDisputes = async { api.getAdminPaymentDisputes() }
                ApiResult.Success(
                    AdminDashboardData(
                        summary = summary.await(),
                        verifications = verifications.await(),
                        reports = reports.await(),
                        tasks = tasks.await(),
                        paymentDisputes = paymentDisputes.await()
                    )
                )
            }
        } catch (e: Exception) {
            ApiResult.Error(e.apiMessage("No se pudo cargar el panel administrativo."))
        }
    }

    suspend fun approveStudentVerification(
        userId: Int,
        observation: String?
    ): ApiResult<StudentVerificationDto> {
        return try {
            ApiResult.Success(
                api.approveStudentVerification(
                    userId,
                    ReviewStudentVerificationRequest(observation)
                )
            )
        } catch (e: Exception) {
            ApiResult.Error(e.apiMessage("No se pudo aprobar la verificacion."))
        }
    }

    suspend fun rejectStudentVerification(
        userId: Int,
        observation: String?
    ): ApiResult<StudentVerificationDto> {
        return try {
            ApiResult.Success(
                api.rejectStudentVerification(
                    userId,
                    ReviewStudentVerificationRequest(observation)
                )
            )
        } catch (e: Exception) {
            ApiResult.Error(e.apiMessage("No se pudo rechazar la verificacion."))
        }
    }

    suspend fun cancelTaskAsAdmin(taskId: Int): ApiResult<TaskDto> {
        return try {
            ApiResult.Success(api.cancelTaskAsAdmin(taskId))
        } catch (e: Exception) {
            ApiResult.Error(e.apiMessage("No se pudo retirar la publicacion."))
        }
    }

    suspend fun reviewReport(
        reportId: Int,
        status: String,
        observation: String?,
        removeTask: Boolean
    ): ApiResult<ReportDto> {
        return try {
            ApiResult.Success(
                api.reviewReport(
                    reportId,
                    ReviewReportRequest(
                        estadoReporte = status,
                        observacion = observation,
                        retirarPublicacion = removeTask
                    )
                )
            )
        } catch (e: Exception) {
            ApiResult.Error(e.apiMessage("No se pudo revisar el reporte."))
        }
    }

    suspend fun resolvePaymentDispute(
        disputeId: Int,
        decision: String,
        resolution: String
    ): ApiResult<PaymentDisputeDto> {
        return try {
            ApiResult.Success(
                api.resolvePaymentDispute(
                    disputeId,
                    ResolvePaymentDisputeRequest(decision, resolution)
                )
            )
        } catch (e: Exception) {
            ApiResult.Error(e.apiMessage("No se pudo resolver la disputa."))
        }
    }

    private suspend fun uploadAttachment(
        attachment: PendingAttachment,
        upload: suspend (MultipartBody.Part) -> AttachmentDto
    ): ApiResult<AttachmentDto> {
        return try {
            val mediaType = attachment.mimeType.toMediaType()
            val localFile = File(attachment.localPath)
            if (!localFile.isFile) {
                return ApiResult.Error("El archivo ${attachment.name} ya no esta disponible.")
            }
            val fileBody = localFile.asRequestBody(mediaType)
            val filePart = MultipartBody.Part.createFormData(
                "file",
                attachment.name,
                fileBody
            )
            ApiResult.Success(upload(filePart)).also {
                localFile.delete()
            }
        } catch (e: Exception) {
            ApiResult.Error(e.apiMessage("No se pudo subir ${attachment.name}."))
        }
    }
}

private fun Exception.apiMessage(fallback: String): String {
    if (this is CancellationException) throw this
    if (this is HttpException) {
        val detail = runCatching {
            val body = response()?.errorBody()?.string().orEmpty()
            JSONObject(body).optString("detail")
        }.getOrNull()
        if (!detail.isNullOrBlank()) {
            return detail
        }
    }
    return message?.takeIf { it.isNotBlank() } ?: fallback
}
