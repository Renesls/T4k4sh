package com.t4kash.app.ui.viewmodel

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.messaging.FirebaseMessaging
import com.t4kash.app.ui.model.AuthResponse
import com.t4kash.app.ui.model.AuthenticatedUserDto
import com.t4kash.app.ui.model.CareerDto
import com.t4kash.app.ui.model.FcmTokenRequest
import com.t4kash.app.ui.model.LoginRequest
import com.t4kash.app.ui.model.RegisterRequest
import com.t4kash.app.ui.model.ResetPasswordRequest
import com.t4kash.app.ui.model.UniversityDto
import com.t4kash.app.ui.model.VerifyEmailRequest
import com.t4kash.app.ui.model.VerifyLoginRequest
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
    val careers: List<CareerDto> = emptyList(),
    val careersUniversityId: Int? = null
)

class AuthViewModel(
    private val repository: AuthRepository = AuthRepository()
) : ViewModel() {
    var uiState by mutableStateOf(AuthUiState())
        private set

    fun login(
        email: String,
        password: String,
        onVerificationRequired: (String) -> Unit
    ) {
        if (uiState.isLoading) return
        viewModelScope.launch {
            uiState = uiState.copy(
                isLoading = true,
                errorMessage = null,
                infoMessage = null
            )
            when (
                val result = repository.login(
                    LoginRequest(email.trim(), password)
                )
            ) {
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
                careersUniversityId = universityId,
                errorMessage = null
            )
            when (val result = repository.getCareers(universityId)) {
                is ApiResult.Success -> {
                    if (uiState.careersUniversityId == universityId) {
                        uiState = uiState.copy(
                            isLoadingOptions = false,
                            careers = result.data
                        )
                    }
                }

                is ApiResult.Error -> {
                    if (uiState.careersUniversityId == universityId) {
                        uiState = uiState.copy(
                            isLoadingOptions = false,
                            errorMessage = result.message
                        )
                    }
                }
            }
        }
    }

    fun clearCareers() {
        uiState = uiState.copy(
            isLoadingOptions = false,
            careers = emptyList(),
            careersUniversityId = null
        )
    }

    fun register(
        firstName: String,
        lastName: String,
        email: String,
        password: String,
        universityId: Int?,
        careerId: Int?,
        studentCard: String?,
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
                    idCarrera = careerId,
                    carnetUniversitario = studentCard?.trim()?.takeIf { it.isNotEmpty() }
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

    fun verifyLogin(
        email: String,
        code: String,
        onSuccess: () -> Unit
    ) {
        executeAuth(
            request = {
                repository.verifyLogin(
                    VerifyLoginRequest(
                        correo = email.trim(),
                        codigo = code.trim()
                    )
                )
            },
            onSuccess = onSuccess
        )
    }

    fun resendLoginVerification(email: String) {
        if (uiState.isLoading) return
        viewModelScope.launch {
            uiState = uiState.copy(
                isLoading = true,
                errorMessage = null,
                infoMessage = null
            )
            when (val result = repository.resendLoginVerification(email.trim())) {
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

    fun requestPasswordReset(
        email: String,
        onCodeRequested: (String) -> Unit
    ) {
        if (uiState.isLoading) return
        viewModelScope.launch {
            uiState = uiState.copy(
                isLoading = true,
                errorMessage = null,
                infoMessage = null
            )
            when (val result = repository.forgotPassword(email.trim())) {
                is ApiResult.Success -> {
                    uiState = uiState.copy(
                        isLoading = false,
                        infoMessage = result.data.mensaje
                    )
                    onCodeRequested(email.trim())
                }

                is ApiResult.Error -> uiState = uiState.copy(
                    isLoading = false,
                    errorMessage = result.message
                )
            }
        }
    }

    fun resetPassword(
        email: String,
        code: String,
        newPassword: String,
        onSuccess: () -> Unit
    ) {
        if (uiState.isLoading) return
        viewModelScope.launch {
            uiState = uiState.copy(
                isLoading = true,
                errorMessage = null,
                infoMessage = null
            )
            when (
                val result = repository.resetPassword(
                    ResetPasswordRequest(
                        correo = email.trim(),
                        codigo = code.trim(),
                        nuevaPassword = newPassword
                    )
                )
            ) {
                is ApiResult.Success -> {
                    UserSession.clear()
                    uiState = uiState.copy(
                        isLoading = false,
                        infoMessage = result.data.mensaje
                    )
                    onSuccess()
                }

                is ApiResult.Error -> uiState = uiState.copy(
                    isLoading = false,
                    errorMessage = result.message
                )
            }
        }
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


                    sincronizarTokenFirebase(result.data.usuario.idUsuario.toLong())


                    onSuccess()
                }

                is ApiResult.Error -> uiState = uiState.copy(
                    isLoading = false,
                    errorMessage = result.message
                )
            }
        }
    }

    private fun sincronizarTokenFirebase(userId: Long) {
        val messaging = try {
            FirebaseMessaging.getInstance()
        } catch (e: IllegalStateException) {
            // Firebase no esta inicializado (falta google-services.json en el modulo app).
            Log.w("FCM_SYNC", "Firebase no esta inicializado, se omite la sincronizacion del token", e)
            return
        }
        messaging.token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w("FCM_SYNC", "Firebase no generó el token", task.exception)
                return@addOnCompleteListener
            }

            val tokenFCM = task.result
            viewModelScope.launch {
                try {
                    val requestBody = FcmTokenRequest(token = tokenFCM)

                    // Llamada al repositorio para enviar el token
                    val exito = repository.enviarTokenFCM(userId, requestBody)

                    if (exito) {
                        Log.d("FCM_SYNC", "Token sincronizado en PostgreSQL")
                    } else {
                        Log.e("FCM_SYNC", "Error del backend guardando token")
                    }
                } catch (e: Exception) {
                    Log.e("FCM_SYNC", "Fallo enviando token al backend", e)
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
        username = nombreUsuario,
        firstName = nombre,
        lastName = apellido,
        email = correo,
        universityName = nombreUniversidad,
        careerName = nombreCarrera,
        accountStatus = estadoUsuario,
        roles = roles.toSet()
    )
}