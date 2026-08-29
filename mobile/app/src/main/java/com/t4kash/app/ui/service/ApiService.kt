package com.t4kash.app.ui.service

import com.t4kash.app.ui.model.FcmTokenRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.PUT
import retrofit2.http.Path

interface ApiService {

    @PUT("api/v1/identity/users/{id}/fcm-token")
    suspend fun enviarTokenFCM(
        @Path("id") userId: Long,
        @Body request: FcmTokenRequest
    ): Response<Unit>
}