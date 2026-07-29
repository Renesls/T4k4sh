package com.t4kash.app.ui.repository

import com.t4kash.app.ui.model.CommunicationOverview
import com.t4kash.app.ui.model.CreateMessageRequest
import com.t4kash.app.ui.model.MessageDto
import com.t4kash.app.ui.model.NotificationDto
import com.t4kash.app.ui.service.ApiResult
import com.t4kash.app.ui.service.CommunicationApiService
import com.t4kash.app.ui.service.RetrofitClient
import org.json.JSONObject
import retrofit2.HttpException

class CommunicationRepository(
    private val api: CommunicationApiService =
        RetrofitClient.communicationApiService
) {
    suspend fun loadOverview(): ApiResult<CommunicationOverview> {
        return try {
            ApiResult.Success(
                CommunicationOverview(
                    conversations = api.getConversations(),
                    notifications = api.getNotifications()
                )
            )
        } catch (e: Exception) {
            ApiResult.Error(
                e.communicationApiMessage(
                    "No se pudo cargar tu actividad."
                )
            )
        }
    }

    suspend fun loadMessages(
        conversationId: Int
    ): ApiResult<List<MessageDto>> {
        return try {
            ApiResult.Success(api.getMessages(conversationId))
        } catch (e: Exception) {
            ApiResult.Error(
                e.communicationApiMessage(
                    "No se pudieron cargar los mensajes."
                )
            )
        }
    }

    suspend fun sendMessage(
        conversationId: Int,
        content: String
    ): ApiResult<MessageDto> {
        return try {
            ApiResult.Success(
                api.sendMessage(
                    conversationId,
                    CreateMessageRequest(content)
                )
            )
        } catch (e: Exception) {
            ApiResult.Error(
                e.communicationApiMessage(
                    "No se pudo enviar el mensaje."
                )
            )
        }
    }

    suspend fun markConversationRead(
        conversationId: Int
    ): ApiResult<Unit> {
        return try {
            api.markConversationRead(conversationId)
            ApiResult.Success(Unit)
        } catch (e: Exception) {
            ApiResult.Error(
                e.communicationApiMessage(
                    "No se pudo actualizar la lectura."
                )
            )
        }
    }

    suspend fun markNotificationRead(
        notificationId: Int
    ): ApiResult<NotificationDto> {
        return try {
            ApiResult.Success(api.markNotificationRead(notificationId))
        } catch (e: Exception) {
            ApiResult.Error(
                e.communicationApiMessage(
                    "No se pudo actualizar la notificacion."
                )
            )
        }
    }

    suspend fun markAllNotificationsRead(): ApiResult<Unit> {
        return try {
            api.markAllNotificationsRead()
            ApiResult.Success(Unit)
        } catch (e: Exception) {
            ApiResult.Error(
                e.communicationApiMessage(
                    "No se pudieron actualizar las notificaciones."
                )
            )
        }
    }
}

private fun Exception.communicationApiMessage(fallback: String): String {
    if (this is HttpException) {
        val detail = runCatching {
            val body = response()?.errorBody()?.string().orEmpty()
            JSONObject(body).optString("detail")
        }.getOrNull()
        if (!detail.isNullOrBlank()) {
            return detail
        }
    }
    return message?.takeIf { it.isNotBlank() } ?: fallback
}
