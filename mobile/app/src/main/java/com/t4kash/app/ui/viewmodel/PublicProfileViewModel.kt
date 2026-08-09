package com.t4kash.app.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.t4kash.app.ui.model.PublicProfileDto
import com.t4kash.app.ui.repository.AuthRepository
import com.t4kash.app.ui.service.ApiResult
import com.t4kash.app.ui.session.UserSession
import kotlinx.coroutines.launch

data class PublicProfileUiState(
    val profile: PublicProfileDto? = null,
    val isLoading: Boolean = false,
    val isUpdating: Boolean = false,
    val errorMessage: String? = null,
    val infoMessage: String? = null
)

class PublicProfileViewModel(
    private val repository: AuthRepository = AuthRepository()
) : ViewModel() {
    var uiState by mutableStateOf(PublicProfileUiState())
        private set

    fun load(username: String) {
        viewModelScope.launch {
            uiState = PublicProfileUiState(isLoading = true)
            when (val result = repository.getPublicProfile(username)) {
                is ApiResult.Success -> uiState = PublicProfileUiState(
                    profile = result.data
                )

                is ApiResult.Error -> uiState = PublicProfileUiState(
                    errorMessage = result.message
                )
            }
        }
    }

    fun updateUsername(username: String, onSuccess: () -> Unit) {
        if (uiState.isUpdating) return
        viewModelScope.launch {
            uiState = uiState.copy(
                isUpdating = true,
                errorMessage = null,
                infoMessage = null
            )
            when (val result = repository.updateUsername(username.trim())) {
                is ApiResult.Success -> {
                    UserSession.current?.user?.let { currentUser ->
                        UserSession.updateUser(
                            currentUser.copy(
                                username = result.data.identidad.nombreUsuario
                            )
                        )
                    }
                    uiState = uiState.copy(
                        profile = result.data,
                        isUpdating = false,
                        infoMessage = "Nombre de usuario actualizado."
                    )
                    onSuccess()
                }

                is ApiResult.Error -> uiState = uiState.copy(
                    isUpdating = false,
                    errorMessage = result.message
                )
            }
        }
    }

    fun clearFeedback() {
        uiState = uiState.copy(errorMessage = null, infoMessage = null)
    }
}
