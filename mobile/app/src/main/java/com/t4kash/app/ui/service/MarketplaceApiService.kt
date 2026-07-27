package com.t4kash.app.ui.service

import com.t4kash.app.ui.model.ApplicationDto
import com.t4kash.app.ui.model.AttachmentDto
import com.t4kash.app.ui.model.CategoryDto
import com.t4kash.app.ui.model.CreateApplicationRequest
import com.t4kash.app.ui.model.CreateDeliveryRequest
import com.t4kash.app.ui.model.CreateTaskRequest
import com.t4kash.app.ui.model.DeliveryDto
import com.t4kash.app.ui.model.JobDto
import com.t4kash.app.ui.model.TaskDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import okhttp3.MultipartBody
import okhttp3.RequestBody

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

    @GET("jobs")
    suspend fun getJobs(): List<JobDto>

    @GET("jobs/{jobId}/deliveries")
    suspend fun getDeliveries(
        @Path("jobId") jobId: Int
    ): List<DeliveryDto>

    @POST("jobs/{jobId}/deliveries")
    suspend fun createDelivery(
        @Path("jobId") jobId: Int,
        @Body request: CreateDeliveryRequest
    ): DeliveryDto

    @POST("deliveries/{deliveryId}/approve")
    suspend fun approveDelivery(
        @Path("deliveryId") deliveryId: Int
    ): DeliveryDto

    @GET("tasks/{taskId}/attachments")
    suspend fun getTaskAttachments(
        @Path("taskId") taskId: Int
    ): List<AttachmentDto>

    @Multipart
    @POST("tasks/{taskId}/attachments")
    suspend fun uploadTaskAttachment(
        @Path("taskId") taskId: Int,
        @Part("userId") userId: RequestBody,
        @Part file: MultipartBody.Part
    ): AttachmentDto

    @GET("jobs/{jobId}/attachments")
    suspend fun getJobAttachments(
        @Path("jobId") jobId: Int
    ): List<AttachmentDto>

    @Multipart
    @POST("deliveries/{deliveryId}/attachments")
    suspend fun uploadDeliveryAttachment(
        @Path("deliveryId") deliveryId: Int,
        @Part("userId") userId: RequestBody,
        @Part file: MultipartBody.Part
    ): AttachmentDto
}
