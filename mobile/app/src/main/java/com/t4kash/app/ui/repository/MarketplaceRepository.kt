package com.t4kash.app.ui.repository

import com.t4kash.app.ui.DemoSession
import com.t4kash.app.ui.model.ApplicationDto
import com.t4kash.app.ui.model.AttachmentDto
import com.t4kash.app.ui.model.CreateApplicationRequest
import com.t4kash.app.ui.model.CreateDeliveryRequest
import com.t4kash.app.ui.model.CreateTaskRequest
import com.t4kash.app.ui.model.DeliveryDto
import com.t4kash.app.ui.model.JobDto
import com.t4kash.app.ui.model.MarketplaceHomeData
import com.t4kash.app.ui.model.PendingAttachment
import com.t4kash.app.ui.model.TaskDto
import com.t4kash.app.ui.service.ApiResult
import com.t4kash.app.ui.service.MarketplaceApiService
import com.t4kash.app.ui.service.RetrofitClient
import org.json.JSONObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.HttpException

class MarketplaceRepository(
    private val api: MarketplaceApiService = RetrofitClient.marketplaceApiService
) {
    suspend fun loadHomeData(): ApiResult<MarketplaceHomeData> {
        return try {
            ApiResult.Success(
                MarketplaceHomeData(
                    categories = api.getCategories(),
                    tasks = api.getTasks()
                )
            )
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "No se pudo conectar con la API.")
        }
    }

    suspend fun createTask(request: CreateTaskRequest): ApiResult<TaskDto> {
        return try {
            ApiResult.Success(api.createTask(request))
        } catch (e: Exception) {
            ApiResult.Error(e.apiMessage("No se pudo publicar la oportunidad."))
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

    suspend fun acceptApplication(applicationId: Int): ApiResult<JobDto> {
        return try {
            ApiResult.Success(api.acceptApplication(applicationId))
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
        return uploadAttachment(attachment) { userId, file ->
            api.uploadTaskAttachment(taskId, userId, file)
        }
    }

    suspend fun uploadDeliveryAttachment(
        deliveryId: Int,
        attachment: PendingAttachment
    ): ApiResult<AttachmentDto> {
        return uploadAttachment(attachment) { userId, file ->
            api.uploadDeliveryAttachment(deliveryId, userId, file)
        }
    }

    private suspend fun uploadAttachment(
        attachment: PendingAttachment,
        upload: suspend (okhttp3.RequestBody, MultipartBody.Part) -> AttachmentDto
    ): ApiResult<AttachmentDto> {
        return try {
            val mediaType = attachment.mimeType.toMediaType()
            val fileBody = attachment.content.toRequestBody(mediaType)
            val filePart = MultipartBody.Part.createFormData(
                "file",
                attachment.name,
                fileBody
            )
            val userId = DemoSession.USER_ID.toString()
                .toRequestBody("text/plain".toMediaType())
            ApiResult.Success(upload(userId, filePart))
        } catch (e: Exception) {
            ApiResult.Error(e.apiMessage("No se pudo subir ${attachment.name}."))
        }
    }
}

private fun Exception.apiMessage(fallback: String): String {
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
