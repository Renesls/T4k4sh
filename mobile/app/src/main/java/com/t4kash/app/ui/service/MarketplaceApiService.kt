package com.t4kash.app.ui.service

import com.t4kash.app.ui.model.ApplicationDto
import com.t4kash.app.ui.model.CategoryDto
import com.t4kash.app.ui.model.CreateApplicationRequest
import com.t4kash.app.ui.model.CreateTaskRequest
import com.t4kash.app.ui.model.JobDto
import com.t4kash.app.ui.model.TaskDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface MarketplaceApiService {
    @GET("categories")
    suspend fun getCategories(): List<CategoryDto>

    @GET("tasks")
    suspend fun getTasks(): List<TaskDto>

    @POST("tasks")
    suspend fun createTask(@Body request: CreateTaskRequest): TaskDto

    @POST("tasks/{taskId}/applications")
    suspend fun applyToTask(
        @Path("taskId") taskId: Int,
        @Body request: CreateApplicationRequest
    ): ApplicationDto

    @GET("tasks/{taskId}/applications")
    suspend fun getApplications(
        @Path("taskId") taskId: Int
    ): List<ApplicationDto>

    @POST("applications/{applicationId}/accept")
    suspend fun acceptApplication(
        @Path("applicationId") applicationId: Int
    ): JobDto

    @POST("applications/{applicationId}/reject")
    suspend fun rejectApplication(
        @Path("applicationId") applicationId: Int
    ): ApplicationDto
}
