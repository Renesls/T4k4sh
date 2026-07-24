package com.t4kash.app.ui.service

import com.t4kash.app.ui.model.CategoryDto
import com.t4kash.app.ui.model.CreateTaskRequest
import com.t4kash.app.ui.model.TaskDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface MarketplaceApiService {
    @GET("categories")
    suspend fun getCategories(): List<CategoryDto>

    @GET("tasks")
    suspend fun getTasks(): List<TaskDto>

    @POST("tasks")
    suspend fun createTask(@Body request: CreateTaskRequest): TaskDto
}
