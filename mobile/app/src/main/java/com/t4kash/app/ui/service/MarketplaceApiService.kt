package com.t4kash.app.ui.service

import com.t4kash.app.ui.model.ApplicationDto
import com.t4kash.app.ui.model.AcceptApplicationRequest
import com.t4kash.app.ui.model.AdminSummaryDto
import com.t4kash.app.ui.model.AttachmentDto
import com.t4kash.app.ui.model.CategoryDto
import com.t4kash.app.ui.model.CreateApplicationRequest
import com.t4kash.app.ui.model.CreateDeliveryRequest
import com.t4kash.app.ui.model.CreateTaskRequest
import com.t4kash.app.ui.model.CreateTaskReportRequest
import com.t4kash.app.ui.model.DeliveryDto
import com.t4kash.app.ui.model.JobDto
import com.t4kash.app.ui.model.CheckoutDto
import com.t4kash.app.ui.model.PaymentDto
import com.t4kash.app.ui.model.WalletDto
import com.t4kash.app.ui.model.ReviewStudentVerificationRequest
import com.t4kash.app.ui.model.ReviewReportRequest
import com.t4kash.app.ui.model.ReportDto
import com.t4kash.app.ui.model.StudentVerificationDto
import com.t4kash.app.ui.model.TaskDto
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.PUT
import okhttp3.MultipartBody

interface MarketplaceApiService {
    @GET("categories")
    suspend fun getCategories(): List<CategoryDto>

    @GET("tasks")
    suspend fun getTasks(): List<TaskDto>

    @POST("tasks")
    suspend fun createTask(@Body request: CreateTaskRequest): TaskDto

    @PUT("tasks/{taskId}")
    suspend fun updateTask(
        @Path("taskId") taskId: Int,
        @Body request: CreateTaskRequest
    ): TaskDto

    @DELETE("tasks/{taskId}")
    suspend fun cancelTask(@Path("taskId") taskId: Int): TaskDto

    @POST("tasks/{taskId}/applications")
    suspend fun applyToTask(
        @Path("taskId") taskId: Int,
        @Body request: CreateApplicationRequest
    ): ApplicationDto

    @GET("tasks/{taskId}/applications")
    suspend fun getApplications(
        @Path("taskId") taskId: Int
    ): List<ApplicationDto>

    @GET("applications/me")
    suspend fun getMyApplications(): List<ApplicationDto>

    @POST("applications/{applicationId}/accept")
    suspend fun acceptApplication(
        @Path("applicationId") applicationId: Int,
        @Body request: AcceptApplicationRequest
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

    @GET("wallet")
    suspend fun getWallet(): WalletDto

    @POST("jobs/{jobId}/payment/checkout")
    suspend fun createPaymentCheckout(
        @Path("jobId") jobId: Int
    ): CheckoutDto

    @POST("payments/{paymentId}/refresh")
    suspend fun refreshPayment(
        @Path("paymentId") paymentId: Int
    ): PaymentDto

    @GET("tasks/{taskId}/attachments")
    suspend fun getTaskAttachments(
        @Path("taskId") taskId: Int
    ): List<AttachmentDto>

    @Multipart
    @POST("tasks/{taskId}/attachments")
    suspend fun uploadTaskAttachment(
        @Path("taskId") taskId: Int,
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
        @Part file: MultipartBody.Part
    ): AttachmentDto

    @Multipart
    @POST("student-verifications/me/attachments")
    suspend fun uploadStudentVerificationAttachment(
        @Part file: MultipartBody.Part
    ): AttachmentDto

    @POST("tasks/{taskId}/reports")
    suspend fun createTaskReport(
        @Path("taskId") taskId: Int,
        @Body request: CreateTaskReportRequest
    ): ReportDto

    @GET("reports/me")
    suspend fun getMyReports(): List<ReportDto>

    @GET("admin/summary")
    suspend fun getAdminSummary(): AdminSummaryDto

    @GET("admin/tasks")
    suspend fun getAdminTasks(): List<TaskDto>

    @GET("admin/reports")
    suspend fun getAdminReports(): List<ReportDto>

    @DELETE("admin/tasks/{taskId}")
    suspend fun cancelTaskAsAdmin(
        @Path("taskId") taskId: Int
    ): TaskDto

    @POST("admin/reports/{reportId}/review")
    suspend fun reviewReport(
        @Path("reportId") reportId: Int,
        @Body request: ReviewReportRequest
    ): ReportDto

    @GET("student-verifications/pending")
    suspend fun getPendingStudentVerifications(): List<StudentVerificationDto>

    @POST("student-verifications/{userId}/approve")
    suspend fun approveStudentVerification(
        @Path("userId") userId: Int,
        @Body request: ReviewStudentVerificationRequest
    ): StudentVerificationDto

    @POST("student-verifications/{userId}/reject")
    suspend fun rejectStudentVerification(
        @Path("userId") userId: Int,
        @Body request: ReviewStudentVerificationRequest
    ): StudentVerificationDto
}
