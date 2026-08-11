package com.t4kash.app.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.t4kash.app.ui.model.ConversationDto
import com.t4kash.app.ui.model.MessageDto
import com.t4kash.app.ui.model.NotificationDto
import com.t4kash.app.ui.repository.CommunicationRepository
import com.t4kash.app.ui.service.ApiResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

data class CommunicationUiState(
    val conversations: List<ConversationDto> = emptyList(),
    val messages: List<MessageDto> = emptyList(),
    val notifications: List<NotificationDto> = emptyList(),
    val activeConversationId: Int? = null,
    val isLoadingOverview: Boolean = false,
    val isLoadingMessages: Boolean = false,
    val isSending: Boolean = false,
    val overviewError: String? = null,
    val messageError: String? = null,
    val sentMessageId: Int? = null
) {
    val unreadMessages: Long
        get() = conversations.sumOf { it.mensajesNoLeidos }

    val unreadNotifications: Int
        get() = notifications.count { !it.leida }
}

class CommunicationViewModel(
    private val repository: CommunicationRepository =
        CommunicationRepository()
) : ViewModel() {
    var uiState by mutableStateOf(CommunicationUiState())
        private set
    private var overviewJob: Job? = null
    private var messageLoadJob: Job? = null

    fun refreshOverview() {
        if (overviewJob?.isActive == true) return
        overviewJob = viewModelScope.launch {
            uiState = uiState.copy(
                isLoadingOverview = true,
                overviewError = null
            )
            when (val result = repository.loadOverview()) {
                is ApiResult.Success -> {
                    uiState = uiState.copy(
                        conversations = result.data.conversations,
                        notifications = result.data.notifications,
                        isLoadingOverview = false,
                        overviewError = null
                    )
                }

                is ApiResult.Error -> {
                    uiState = uiState.copy(
                        isLoadingOverview = false,
                        overviewError = result.message
                    )
                }
            }
        }
    }

    fun loadMessages(
        conversationId: Int,
        silent: Boolean = false
    ) {
        if (uiState.activeConversationId != conversationId) {
            messageLoadJob?.cancel()
        }
        if (messageLoadJob?.isActive == true) return
        messageLoadJob = viewModelScope.launch {
            uiState = uiState.copy(
                activeConversationId = conversationId,
                isLoadingMessages = !silent,
                messageError = null
            )
            when (val result = repository.loadMessages(conversationId)) {
                is ApiResult.Success -> {
                    if (uiState.activeConversationId != conversationId) return@launch
                    uiState = uiState.copy(
                        messages = result.data,
                        isLoadingMessages = false,
                        messageError = null,
                        conversations = uiState.conversations.map { item ->
                            if (item.idConversacion == conversationId) {
                                item.copy(mensajesNoLeidos = 0)
                            } else {
                                item
                            }
                        }
                    )
                    repository.markConversationRead(conversationId)
                }

                is ApiResult.Error -> {
                    if (uiState.activeConversationId != conversationId) return@launch
                    uiState = uiState.copy(
                        isLoadingMessages = false,
                        messageError = result.message
                    )
                }
            }
        }
    }

    fun sendMessage(conversationId: Int, content: String) {
        val cleanContent = content.trim()
        if (cleanContent.isBlank() || uiState.isSending) {
            return
        }
        messageLoadJob?.cancel()
        viewModelScope.launch {
            uiState = uiState.copy(
                isSending = true,
                messageError = null,
                sentMessageId = null
            )
            when (
                val result = repository.sendMessage(
                    conversationId,
                    cleanContent
                )
            ) {
                is ApiResult.Success -> {
                    uiState = uiState.copy(
                        isSending = false,
                        messages = uiState.messages + result.data,
                        sentMessageId = result.data.idMensaje,
                        conversations = uiState.conversations.map { item ->
                            if (item.idConversacion == conversationId) {
                                item.copy(
                                    ultimoMensaje = result.data.contenido,
                                    fechaUltimoMensaje =
                                        result.data.fechaEnvio
                                )
                            } else {
                                item
                            }
                        }
                    )
                }

                is ApiResult.Error -> {
                    uiState = uiState.copy(
                        isSending = false,
                        messageError = result.message
                    )
                }
            }
        }
    }

    fun markNotificationRead(notificationId: Int) {
        val current = uiState.notifications.firstOrNull {
            it.idNotificacion == notificationId
        } ?: return
        if (current.leida) return

        viewModelScope.launch {
            when (val result = repository.markNotificationRead(notificationId)) {
                is ApiResult.Success -> {
                    uiState = uiState.copy(
                        notifications = uiState.notifications.map { item ->
                            if (item.idNotificacion == notificationId) {
                                result.data
                            } else {
                                item
                            }
                        }
                    )
                }

                is ApiResult.Error -> {
                    uiState = uiState.copy(overviewError = result.message)
                }
            }
        }
    }

    fun markAllNotificationsRead() {
        if (uiState.unreadNotifications == 0) return
        viewModelScope.launch {
            when (val result = repository.markAllNotificationsRead()) {
                is ApiResult.Success -> {
                    uiState = uiState.copy(
                        notifications = uiState.notifications.map {
                            it.copy(leida = true)
                        }
                    )
                }

                is ApiResult.Error -> {
                    uiState = uiState.copy(overviewError = result.message)
                }
            }
        }
    }

    fun clearSendFeedback() {
        uiState = uiState.copy(sentMessageId = null)
    }

    fun clearSession() {
        overviewJob?.cancel()
        messageLoadJob?.cancel()
        uiState = CommunicationUiState()
    }
}
