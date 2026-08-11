package com.t4kash.app.ui.service

import com.t4kash.app.ui.model.ConversationDto
import com.t4kash.app.ui.model.CreateMessageRequest
import com.t4kash.app.ui.model.MessageDto
import com.t4kash.app.ui.model.NotificationDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface CommunicationApiService {
    @GET("conversations")
    suspend fun getConversations(): List<ConversationDto>

    @GET("conversations/{conversationId}/messages")
    suspend fun getMessages(
        @Path("conversationId") conversationId: Int,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 100
    ): List<MessageDto>

    @POST("conversations/{conversationId}/messages")
    suspend fun sendMessage(
        @Path("conversationId") conversationId: Int,
        @Body request: CreateMessageRequest
    ): MessageDto

    @POST("conversations/{conversationId}/read")
    suspend fun markConversationRead(
        @Path("conversationId") conversationId: Int
    )

    @GET("notifications")
    suspend fun getNotifications(
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 50
    ): List<NotificationDto>

    @POST("notifications/{notificationId}/read")
    suspend fun markNotificationRead(
        @Path("notificationId") notificationId: Int
    ): NotificationDto

    @POST("notifications/read-all")
    suspend fun markAllNotificationsRead()
}
