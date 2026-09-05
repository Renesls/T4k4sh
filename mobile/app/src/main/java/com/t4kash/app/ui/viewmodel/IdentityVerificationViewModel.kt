package com.t4kash.app.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.t4kash.app.ui.model.IdentityVerificationStatusDto
import com.t4kash.app.ui.repository.IdentityVerificationRepository
import com.t4kash.app.ui.service.ApiResult
import kotlinx.coroutines.launch

data class IdentityVerificationUiState(
    val status: IdentityVerificationStatusDto? = null,
    val isLoading: Boolean = false,
    val isStarting: Boolean = false,
    val isRefreshing: Boolean = false,
    val verificationUrl: String? = null,
    val errorMessage: String? = null
)

class IdentityVerificationViewModel(
    private val repository: IdentityVerificationRepository =
        IdentityVerificationRepository()
) : ViewModel() {
    var uiState by mutableStateOf(IdentityVerificationUiState())
        private set

    fun load(force: Boolean = false) {
        if (uiState.isLoading || (!force && uiState.status != null)) return
        viewModelScope.launch {
            uiState = uiState.copy(isLoading = true, errorMessage = null)
            when (val result = repository.getStatus()) {
                is ApiResult.Success -> uiState = uiState.copy(
                    isLoading = false,
                    status = result.data
                )

                is ApiResult.Error -> uiState = uiState.copy(
                    isLoading = false,
                    errorMessage = result.message
                )
            }
        }
    }

    fun start() {
        if (uiState.isStarting) return
        viewModelScope.launch {
            uiState = uiState.copy(isStarting = true, errorMessage = null)
            when (val result = repository.startSession()) {
                is ApiResult.Success -> {
                    uiState = uiState.copy(
                        isStarting = false,
                        verificationUrl = result.data.urlVerificacion
                    )
                    if (result.data.urlVerificacion == null) {
                        load(force = true)
                    }
                }

                is ApiResult.Error -> uiState = uiState.copy(
                    isStarting = false,
                    errorMessage = result.message
                )
            }
        }
    }

    fun refresh() {
        if (uiState.isRefreshing) return
        viewModelScope.launch {
            uiState = uiState.copy(isRefreshing = true, errorMessage = null)
            when (val result = repository.refreshStatus()) {
                is ApiResult.Success -> uiState = uiState.copy(
                    isRefreshing = false,
                    status = result.data
                )

                is ApiResult.Error -> uiState = uiState.copy(
                    isRefreshing = false,
                    errorMessage = result.message
                )
            }
        }
    }

    fun consumeVerificationUrl() {
        if (uiState.verificationUrl != null) {
            uiState = uiState.copy(verificationUrl = null)
        }
    }

    fun clearError() {
        if (uiState.errorMessage != null) {
            uiState = uiState.copy(errorMessage = null)
        }
    }

    fun clearSession() {
        uiState = IdentityVerificationUiState()
    }
}
