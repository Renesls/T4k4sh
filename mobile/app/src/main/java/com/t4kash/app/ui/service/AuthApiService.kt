package com.t4kash.app.ui.service

import com.t4kash.app.ui.model.AuthResponse
import com.t4kash.app.ui.model.AuthenticatedUserDto
import com.t4kash.app.ui.model.CareerDto
import com.t4kash.app.ui.model.LoginRequest
import com.t4kash.app.ui.model.RegisterRequest
import com.t4kash.app.ui.model.RegistrationResponse
import com.t4kash.app.ui.model.ResendVerificationRequest
import com.t4kash.app.ui.model.UniversityDto
import com.t4kash.app.ui.model.VerifyEmailRequest
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface AuthApiService {
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): AuthResponse

    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): RegistrationResponse

    @POST("auth/verify-email")
    suspend fun verifyEmail(@Body request: VerifyEmailRequest): AuthResponse

    @POST("auth/resend-verification")
    suspend fun resendVerification(
        @Body request: ResendVerificationRequest
    ): RegistrationResponse

    @GET("auth/me")
    suspend fun getCurrentUser(): AuthenticatedUserDto

    @POST("auth/logout")
    suspend fun logout()

    @GET("identity/universities")
    suspend fun getUniversities(): List<UniversityDto>

    @GET("identity/universities/{universityId}/careers")
    suspend fun getCareers(
        @retrofit2.http.Path("universityId") universityId: Int
    ): List<CareerDto>
}
