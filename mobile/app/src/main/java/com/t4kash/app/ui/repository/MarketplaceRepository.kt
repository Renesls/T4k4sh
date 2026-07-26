package com.t4kash.app.ui.repository

import com.t4kash.app.ui.model.ApplicationDto
import com.t4kash.app.ui.model.CreateApplicationRequest
import com.t4kash.app.ui.model.CreateTaskRequest
import com.t4kash.app.ui.model.MarketplaceHomeData
import com.t4kash.app.ui.model.TaskDto
import com.t4kash.app.ui.service.ApiResult
import com.t4kash.app.ui.service.MarketplaceApiService
import com.t4kash.app.ui.service.RetrofitClient
import org.json.JSONObject
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
