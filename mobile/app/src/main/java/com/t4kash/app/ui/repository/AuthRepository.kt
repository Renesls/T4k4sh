package com.t4kash.app.ui.repository

import com.t4kash.app.ui.model.AuthResponse
import com.t4kash.app.ui.model.AuthenticatedUserDto
import com.t4kash.app.ui.model.CareerDto
import com.t4kash.app.ui.model.LoginRequest
import com.t4kash.app.ui.model.RegisterRequest
import com.t4kash.app.ui.model.RegistrationResponse
import com.t4kash.app.ui.model.ResendVerificationRequest
import com.t4kash.app.ui.model.UniversityDto
import com.t4kash.app.ui.model.VerifyEmailRequest
import com.t4kash.app.ui.service.ApiResult
import com.t4kash.app.ui.service.AuthApiService
import com.t4kash.app.ui.service.RetrofitClient
import java.net.SocketTimeoutException
import org.json.JSONObject
import retrofit2.HttpException

class AuthRepository(
    private val api: AuthApiService = RetrofitClient.authApiService
) {
    suspend fun login(request: LoginRequest): ApiResult<AuthResponse> = execute {
        api.login(request)
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

    suspend fun logout(): ApiResult<Unit> = execute {
        api.logout()
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
