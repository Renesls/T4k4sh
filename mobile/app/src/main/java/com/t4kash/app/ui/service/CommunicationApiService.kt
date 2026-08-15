package com.t4kash.app.ui.service

import com.t4kash.app.ui.model.ConversationDto
import com.t4kash.app.ui.model.CreateMessageRequest
import com.t4kash.app.ui.model.MessageDto
import com.t4kash.app.ui.model.NotificationDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface CommunicationApiService {
    @GET("conversations")
    suspend fun getConversations(): List<ConversationDto>

    @GET("conversations/{conversationId}/messages")
    suspend fun getMessages(
        @Path("conversationId") conversationId: Int
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
    suspend fun getNotifications(): List<NotificationDto>

    @POST("notifications/{notificationId}/read")
    suspend fun markNotificationRead(
        @Path("notificationId") notificationId: Int
    ): NotificationDto

    @POST("notifications/read-all")
    suspend fun markAllNotificationsRead()
}
