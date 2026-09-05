package com.t4kash.app.ui.repository

import com.t4kash.app.ui.model.AuthResponse
import com.t4kash.app.ui.model.AuthenticatedUserDto
import com.t4kash.app.ui.model.CareerDto
import com.t4kash.app.ui.model.ForgotPasswordRequest
import com.t4kash.app.ui.model.LoginChallengeResponse
import com.t4kash.app.ui.model.LoginRequest
import com.t4kash.app.ui.model.MessageResponse
import com.t4kash.app.ui.model.PublicProfileDto
import com.t4kash.app.ui.model.RegisterRequest
import com.t4kash.app.ui.model.RegistrationResponse
import com.t4kash.app.ui.model.ResendVerificationRequest
import com.t4kash.app.ui.model.ResetPasswordRequest
import com.t4kash.app.ui.model.UniversityDto
import com.t4kash.app.ui.model.UpdateUsernameRequest
import com.t4kash.app.ui.model.VerifyEmailRequest
import com.t4kash.app.ui.model.VerifyLoginRequest
import com.t4kash.app.ui.service.ApiResult
import com.t4kash.app.ui.service.AuthApiService
import com.t4kash.app.ui.service.RetrofitClient
import java.net.SocketTimeoutException
import kotlinx.coroutines.CancellationException
import org.json.JSONObject
import retrofit2.HttpException
import com.t4kash.app.ui.model.FcmTokenRequest

class AuthRepository(
    private val api: AuthApiService = RetrofitClient.authApiService
) {
    suspend fun login(
        request: LoginRequest
    ): ApiResult<LoginChallengeResponse> = execute {
        api.login(request)
    }

    suspend fun verifyLogin(
        request: VerifyLoginRequest
    ): ApiResult<AuthResponse> = execute {
        api.verifyLogin(request)
    }

    suspend fun resendLoginVerification(
        email: String
    ): ApiResult<LoginChallengeResponse> = execute {
        api.resendLoginVerification(ResendVerificationRequest(email))
    }

    suspend fun register(
        request: RegisterRequest
    ): ApiResult<RegistrationResponse> = execute {
        api.register(request)
    }

    suspend fun verifyEmail(
        request: VerifyEmailRequest
    ): ApiResult<AuthResponse> = execute {
        api.verifyEmail(request)
    }

    suspend fun resendVerification(
        email: String
    ): ApiResult<RegistrationResponse> = execute {
        api.resendVerification(ResendVerificationRequest(email))
    }

    suspend fun forgotPassword(email: String): ApiResult<MessageResponse> = execute {
        api.forgotPassword(ForgotPasswordRequest(email))
    }

    suspend fun resetPassword(
        request: ResetPasswordRequest
    ): ApiResult<MessageResponse> = execute {
        api.resetPassword(request)
    }

    suspend fun getUniversities(): ApiResult<List<UniversityDto>> = execute {
        api.getUniversities()
    }

    suspend fun getCareers(
        universityId: Int
    ): ApiResult<List<CareerDto>> = execute {
        api.getCareers(universityId)
    }

    suspend fun getCurrentUser(): ApiResult<AuthenticatedUserDto> = execute {
        api.getCurrentUser()
    }

    suspend fun getPublicProfile(username: String): ApiResult<PublicProfileDto> = execute {
        api.getPublicProfile(username)
    }

    suspend fun updateUsername(username: String): ApiResult<PublicProfileDto> = execute {
        api.updateUsername(UpdateUsernameRequest(username))
    }

    suspend fun logout(): ApiResult<Unit> = execute {
        api.logout()
    }

    suspend fun enviarTokenFCM(userId: Long, request: FcmTokenRequest): Boolean {
        return try {
            val response = api.enviarTokenFCM(userId, request)
            response.isSuccessful
        } catch (e: Exception) {
            false
        }
    }
    private suspend fun <T> execute(block: suspend () -> T): ApiResult<T> {
        return try {
            ApiResult.Success(block())
        } catch (exception: Exception) {
            ApiResult.Error(exception.authMessage())
        }
    }
}

private fun Exception.authMessage(): String {
    if (this is CancellationException) throw this
    if (this is SocketTimeoutException) {
        return "La solicitud tardo demasiado. Intenta nuevamente."
    }
    if (this is HttpException) {
        val detail = runCatching {
            val body = response()?.errorBody()?.string().orEmpty()
            JSONObject(body).optString("detail")
        }.getOrNull()
        if (!detail.isNullOrBlank()) return detail
    }
    return message?.takeIf { it.isNotBlank() }
        ?: "No se pudo conectar con el servicio de identidad."
}
