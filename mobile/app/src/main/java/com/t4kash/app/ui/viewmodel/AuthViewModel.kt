package com.t4kash.app.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.t4kash.app.ui.model.AuthResponse
import com.t4kash.app.ui.model.AuthenticatedUserDto
import com.t4kash.app.ui.model.LoginRequest
import com.t4kash.app.ui.model.RegisterRequest
import com.t4kash.app.ui.repository.AuthRepository
import com.t4kash.app.ui.service.ApiResult
import com.t4kash.app.ui.session.AuthSession
import com.t4kash.app.ui.session.SessionUser
import com.t4kash.app.ui.session.UserSession
import kotlinx.coroutines.launch

data class AuthUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class AuthViewModel(
    private val repository: AuthRepository = AuthRepository()
) : ViewModel() {
    var uiState by mutableStateOf(AuthUiState())
        private set

    fun login(
        email: String,
        password: String,
        onSuccess: () -> Unit
    ) {
        execute(
            request = { repository.login(LoginRequest(email.trim(), password)) },
            onSuccess = onSuccess
        )
    }

    fun register(
        firstName: String,
        lastName: String,
        email: String,
        password: String,
        onSuccess: () -> Unit
    ) {
        execute(
            request = {
                repository.register(
                    RegisterRequest(
                        nombre = firstName.trim(),
                        apellido = lastName.trim(),
                        correo = email.trim(),
                        password = password
                    )
                )
            },
            onSuccess = onSuccess
        )
    }

    fun validateStoredSession(onInvalid: () -> Unit = {}) {
        if (UserSession.current == null) return
        viewModelScope.launch {
            when (val result = repository.getCurrentUser()) {
                is ApiResult.Success -> UserSession.updateUser(result.data.toSessionUser())
                is ApiResult.Error -> {
                    if (UserSession.current == null) {
                        onInvalid()
                    }
                }
            }
        }
    }

    fun logout(onFinished: () -> Unit) {
        viewModelScope.launch {
            if (UserSession.current != null) {
                repository.logout()
            }
            UserSession.clear()
            uiState = AuthUiState()
            onFinished()
        }
    }

    fun clearError() {
        if (uiState.errorMessage != null) {
            uiState = uiState.copy(errorMessage = null)
        }
    }

    private fun execute(
        request: suspend () -> ApiResult<AuthResponse>,
        onSuccess: () -> Unit
    ) {
        if (uiState.isLoading) return
        viewModelScope.launch {
            uiState = AuthUiState(isLoading = true)
            when (val result = request()) {
                is ApiResult.Success -> {
                    UserSession.save(result.data.toSession())
                    uiState = AuthUiState()
                    onSuccess()
                }

                is ApiResult.Error -> {
                    uiState = AuthUiState(errorMessage = result.message)
                }
            }
        }
    }
}

private fun AuthResponse.toSession(): AuthSession {
    return AuthSession(
        token = token,
        expiresAt = fechaExpiracion,
        user = usuario.toSessionUser()
    )
}

private fun AuthenticatedUserDto.toSessionUser(): SessionUser {
    return SessionUser(
        id = idUsuario,
        firstName = nombre,
        lastName = apellido,
        email = correo,
        accountStatus = estadoUsuario,
        roles = roles.toSet()
    )
}
