package com.t4kash.app.ui.repository

import com.t4kash.app.ui.model.CreateTaskRequest
import com.t4kash.app.ui.model.MarketplaceHomeData
import com.t4kash.app.ui.model.TaskDto
import com.t4kash.app.ui.service.ApiResult
import com.t4kash.app.ui.service.MarketplaceApiService
import com.t4kash.app.ui.service.RetrofitClient

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
            ApiResult.Error(e.message ?: "No se pudo publicar la oportunidad.")
        }
    }
}
