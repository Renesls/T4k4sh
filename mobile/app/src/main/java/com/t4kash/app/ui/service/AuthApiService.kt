package com.t4kash.app.ui.service

import com.t4kash.app.ui.model.AuthResponse
import com.t4kash.app.ui.model.AuthenticatedUserDto
import com.t4kash.app.ui.model.CareerDto
import com.t4kash.app.ui.model.ForgotPasswordRequest
import com.t4kash.app.ui.model.LoginChallengeResponse
import com.t4kash.app.ui.model.LoginRequest
import com.t4kash.app.ui.model.MessageResponse
import com.t4kash.app.ui.model.RegisterRequest
import com.t4kash.app.ui.model.RegistrationResponse
import com.t4kash.app.ui.model.ResendVerificationRequest
import com.t4kash.app.ui.model.ResetPasswordRequest
import com.t4kash.app.ui.model.UniversityDto
import com.t4kash.app.ui.model.VerifyEmailRequest
import com.t4kash.app.ui.model.VerifyLoginRequest
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface AuthApiService {
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): LoginChallengeResponse

    @POST("auth/login/verify")
    suspend fun verifyLogin(@Body request: VerifyLoginRequest): AuthResponse

    @POST("auth/login/resend")
    suspend fun resendLoginVerification(
        @Body request: ResendVerificationRequest
    ): LoginChallengeResponse

    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): RegistrationResponse

    @POST("auth/verify-email")
    suspend fun verifyEmail(@Body request: VerifyEmailRequest): AuthResponse

    @POST("auth/resend-verification")
    suspend fun resendVerification(
        @Body request: ResendVerificationRequest
    ): RegistrationResponse

    @POST("auth/password/forgot")
    suspend fun forgotPassword(
        @Body request: ForgotPasswordRequest
    ): MessageResponse

    @POST("auth/password/reset")
    suspend fun resetPassword(
        @Body request: ResetPasswordRequest
    ): MessageResponse

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
