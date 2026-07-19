package com.t4kash.app.ui.service

import com.t4kash.app.ui.model.CategoryDto
import com.t4kash.app.ui.model.TaskDto
import retrofit2.http.GET

interface MarketplaceApiService {
    @GET("categories")
    suspend fun getCategories(): List<CategoryDto>

    @GET("tasks")
    suspend fun getTasks(): List<TaskDto>
}
