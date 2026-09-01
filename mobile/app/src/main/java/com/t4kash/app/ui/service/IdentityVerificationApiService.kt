package com.t4kash.app.ui.service

import com.t4kash.app.ui.model.IdentityVerificationSessionDto
import com.t4kash.app.ui.model.IdentityVerificationStatusDto
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface IdentityVerificationApiService {
    @GET("identity-verifications/me")
    suspend fun getStatus(): IdentityVerificationStatusDto

    @POST("identity-verifications/me/session")
    suspend fun startSession(
        @Query("origen") origin: String = "PERFIL"
    ): IdentityVerificationSessionDto

    @POST("identity-verifications/me/refresh")
    suspend fun refreshStatus(): IdentityVerificationStatusDto
}
