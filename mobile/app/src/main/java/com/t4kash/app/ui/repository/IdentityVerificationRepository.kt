package com.t4kash.app.ui.repository

import com.t4kash.app.ui.model.IdentityVerificationSessionDto
import com.t4kash.app.ui.model.IdentityVerificationStatusDto
import com.t4kash.app.ui.service.ApiResult
import com.t4kash.app.ui.service.IdentityVerificationApiService
import com.t4kash.app.ui.service.RetrofitClient
import java.net.SocketTimeoutException
import kotlinx.coroutines.CancellationException
import org.json.JSONObject
import retrofit2.HttpException

class IdentityVerificationRepository(
    private val api: IdentityVerificationApiService =
        RetrofitClient.identityVerificationApiService
) {
    suspend fun getStatus(): ApiResult<IdentityVerificationStatusDto> = execute {
        api.getStatus()
    }

    suspend fun startSession(): ApiResult<IdentityVerificationSessionDto> = execute {
        api.startSession()
    }

    suspend fun refreshStatus(): ApiResult<IdentityVerificationStatusDto> = execute {
        api.refreshStatus()
    }

    private suspend fun <T> execute(block: suspend () -> T): ApiResult<T> {
        return try {
            ApiResult.Success(block())
        } catch (exception: Exception) {
            ApiResult.Error(exception.identityMessage())
        }
    }
}

private fun Exception.identityMessage(): String {
    if (this is CancellationException) throw this
    if (this is SocketTimeoutException) {
        return "La consulta tardo demasiado. Intenta nuevamente."
    }
    if (this is HttpException) {
        val detail = runCatching {
            val body = response()?.errorBody()?.string().orEmpty()
            JSONObject(body).optString("detail")
        }.getOrNull()
        if (!detail.isNullOrBlank()) return detail
    }
    return message?.takeIf { it.isNotBlank() }
        ?: "No se pudo consultar la verificacion de identidad."
}
