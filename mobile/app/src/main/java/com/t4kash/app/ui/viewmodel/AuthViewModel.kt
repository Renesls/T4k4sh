package com.t4kash.app.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.t4kash.app.ui.model.AuthResponse
import com.t4kash.app.ui.model.AuthenticatedUserDto
import com.t4kash.app.ui.model.CareerDto
import com.t4kash.app.ui.model.LoginRequest
import com.t4kash.app.ui.model.RegisterRequest
import com.t4kash.app.ui.model.UniversityDto
import com.t4kash.app.ui.model.VerifyEmailRequest
import com.t4kash.app.ui.repository.AuthRepository
import com.t4kash.app.ui.service.ApiResult
import com.t4kash.app.ui.session.AuthSession
import com.t4kash.app.ui.session.SessionUser
import com.t4kash.app.ui.session.UserSession
import kotlinx.coroutines.launch

data class AuthUiState(
    val isLoading: Boolean = false,
    val isLoadingOptions: Boolean = false,
    val errorMessage: String? = null,
    val infoMessage: String? = null,
    val universities: List<UniversityDto> = emptyList(),
    val careers: List<CareerDto> = emptyList()
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
        executeAuth(
            request = { repository.login(LoginRequest(email.trim(), password)) },
            onSuccess = onSuccess
        )
    }

    fun loadUniversities() {
        if (uiState.universities.isNotEmpty() || uiState.isLoadingOptions) return
        viewModelScope.launch {
            uiState = uiState.copy(isLoadingOptions = true, errorMessage = null)
            when (val result = repository.getUniversities()) {
                is ApiResult.Success -> uiState = uiState.copy(
                    isLoadingOptions = false,
                    universities = result.data
                )

                is ApiResult.Error -> uiState = uiState.copy(
                    isLoadingOptions = false,
                    errorMessage = result.message
                )
            }
        }
    }

    fun loadCareers(universityId: Int) {
        viewModelScope.launch {
            uiState = uiState.copy(
                isLoadingOptions = true,
                careers = emptyList(),
                errorMessage = null
            )
            when (val result = repository.getCareers(universityId)) {
                is ApiResult.Success -> uiState = uiState.copy(
                    isLoadingOptions = false,
                    careers = result.data
                )

                is ApiResult.Error -> uiState = uiState.copy(
                    isLoadingOptions = false,
                    errorMessage = result.message
                )
            }
        }
    }

    fun register(
        firstName: String,
        lastName: String,
        email: String,
        password: String,
        universityId: Int,
        careerId: Int,
        onVerificationRequired: (String) -> Unit
    ) {
        if (uiState.isLoading) return
        viewModelScope.launch {
            uiState = uiState.copy(
                isLoading = true,
                errorMessage = null,
                infoMessage = null
            )
            val result = repository.register(
                RegisterRequest(
                    nombre = firstName.trim(),
                    apellido = lastName.trim(),
                    correo = email.trim(),
                    password = password,
                    idUniversidad = universityId,
                    idCarrera = careerId
                )
            )
            when (result) {
                is ApiResult.Success -> {
                    uiState = uiState.copy(
                        isLoading = false,
                        infoMessage = result.data.mensaje
                    )
                    onVerificationRequired(result.data.correo)
                }

                is ApiResult.Error -> uiState = uiState.copy(
                    isLoading = false,
                    errorMessage = result.message
                )
            }
        }
    }

    fun verifyEmail(
        email: String,
        code: String,
        onSuccess: () -> Unit
    ) {
        executeAuth(
            request = {
                repository.verifyEmail(
                    VerifyEmailRequest(
                        correo = email.trim(),
                        codigo = code.trim()
                    )
                )
            },
            onSuccess = onSuccess
        )
    }

    fun resendVerification(email: String) {
        if (uiState.isLoading) return
        viewModelScope.launch {
            uiState = uiState.copy(
                isLoading = true,
                errorMessage = null,
                infoMessage = null
            )
            when (val result = repository.resendVerification(email.trim())) {
                is ApiResult.Success -> uiState = uiState.copy(
                    isLoading = false,
                    infoMessage = result.data.mensaje
                )

                is ApiResult.Error -> uiState = uiState.copy(
                    isLoading = false,
                    errorMessage = result.message
                )
            }
        }
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
        if (uiState.errorMessage != null || uiState.infoMessage != null) {
            uiState = uiState.copy(errorMessage = null, infoMessage = null)
        }
    }

    private fun executeAuth(
        request: suspend () -> ApiResult<AuthResponse>,
        onSuccess: () -> Unit
    ) {
        if (uiState.isLoading) return
        viewModelScope.launch {
            uiState = uiState.copy(
                isLoading = true,
                errorMessage = null,
                infoMessage = null
            )
            when (val result = request()) {
                is ApiResult.Success -> {
                    UserSession.save(result.data.toSession())
                    uiState = uiState.copy(isLoading = false)
                    onSuccess()
                }

                is ApiResult.Error -> uiState = uiState.copy(
                    isLoading = false,
                    errorMessage = result.message
                )
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
        universityName = nombreUniversidad,
        careerName = nombreCarrera,
        accountStatus = estadoUsuario,
        roles = roles.toSet()
    )
}
