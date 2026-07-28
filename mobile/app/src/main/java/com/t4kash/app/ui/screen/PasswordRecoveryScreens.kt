package com.t4kash.app.ui.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.t4kash.app.ui.components.T4PatternSurface
import com.t4kash.app.ui.components.T4TopBar
import com.t4kash.app.ui.theme.T4Background
import com.t4kash.app.ui.theme.T4Border
import com.t4kash.app.ui.theme.T4Mint
import com.t4kash.app.ui.theme.T4Surface
import com.t4kash.app.ui.viewmodel.AuthViewModel

@Composable
fun ForgotPasswordScreen(
    viewModel: AuthViewModel,
    onBack: () -> Unit,
    onCodeRequested: (String) -> Unit
) {
    var email by rememberSaveable { mutableStateOf("") }
    var validationError by rememberSaveable { mutableStateOf<String?>(null) }
    val uiState = viewModel.uiState

    fun submit() {
        validationError = if (email.isBlank() || !email.contains("@")) {
            "Ingresa un correo valido."
        } else {
            null
        }
        if (validationError == null) {
            viewModel.requestPasswordReset(email, onCodeRequested)
        }
    }

    RecoveryLayout(
        title = "Recuperar cuenta",
        subtitle = "Solicita un codigo temporal",
        heading = "Crea una nueva contrasena",
        description = "Escribe el correo de tu cuenta. Si existe, enviaremos un codigo de recuperacion.",
        onBack = onBack
    ) {
        OutlinedTextField(
            value = email,
            onValueChange = {
                email = it
                validationError = null
                viewModel.clearError()
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Correo") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
        )
        AuthMessage(validationError ?: uiState.errorMessage)
        LoadingButton(
            text = "Enviar codigo",
            isLoading = uiState.isLoading,
            onClick = ::submit
        )
    }
}

@Composable
fun ResetPasswordScreen(
    initialEmail: String,
    viewModel: AuthViewModel,
    onBack: () -> Unit,
    onSuccess: () -> Unit
) {
    var code by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirmation by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    var validationError by rememberSaveable { mutableStateOf<String?>(null) }
    val uiState = viewModel.uiState

    fun submit() {
        validationError = when {
            code.length != 6 -> "Ingresa el codigo de 6 digitos."
            password.length !in 8..72 ->
                "La contrasena debe tener entre 8 y 72 caracteres."
            password != confirmation -> "Las contrasenas no coinciden."
            else -> null
        }
        if (validationError == null) {
            viewModel.resetPassword(
                initialEmail,
                code,
                password,
                onSuccess
            )
        }
    }

    RecoveryLayout(
        title = "Nueva contrasena",
        subtitle = initialEmail,
        heading = "Confirma el cambio",
        description = "Usa el codigo recibido y elige una contrasena que no utilices en otros servicios.",
        onBack = onBack
    ) {
        OutlinedTextField(
            value = code,
            onValueChange = {
                code = it.filter(Char::isDigit).take(6)
                validationError = null
                viewModel.clearError()
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Codigo de 6 digitos") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.NumberPassword
            )
        )
        PasswordField(
            value = password,
            label = "Nueva contrasena",
            visible = passwordVisible,
            onVisibilityChange = { passwordVisible = !passwordVisible },
            onValueChange = {
                password = it
                validationError = null
                viewModel.clearError()
            }
        )
        PasswordField(
            value = confirmation,
            label = "Confirmar contrasena",
            visible = passwordVisible,
            onVisibilityChange = { passwordVisible = !passwordVisible },
            onValueChange = {
                confirmation = it
                validationError = null
                viewModel.clearError()
            }
        )
        AuthMessage(validationError ?: uiState.errorMessage)
        LoadingButton(
            text = "Actualizar contrasena",
            isLoading = uiState.isLoading,
            onClick = ::submit
        )
    }
}

@Composable
private fun RecoveryLayout(
    title: String,
    subtitle: String,
    heading: String,
    description: String,
    onBack: () -> Unit,
    content: @Composable () -> Unit
) {
    Scaffold(
        containerColor = T4Background,
        topBar = {
            T4TopBar(title = title, subtitle = subtitle, onBack = onBack)
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                T4PatternSurface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.LockReset,
                            contentDescription = null,
                            tint = T4Mint
                        )
                        Text(
                            text = heading,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = description,
                            color = Color.White.copy(alpha = 0.84f)
                        )
                    }
                }
            }
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = T4Surface),
                    border = BorderStroke(1.dp, T4Border.copy(alpha = 0.65f))
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        content()
                    }
                }
            }
        }
    }
}

@Composable
private fun PasswordField(
    value: String,
    label: String,
    visible: Boolean,
    onVisibilityChange: () -> Unit,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        singleLine = true,
        visualTransformation = if (visible) {
            VisualTransformation.None
        } else {
            PasswordVisualTransformation()
        },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        trailingIcon = {
            IconButton(onClick = onVisibilityChange) {
                Icon(
                    imageVector = if (visible) {
                        Icons.Filled.VisibilityOff
                    } else {
                        Icons.Filled.Visibility
                    },
                    contentDescription = null
                )
            }
        }
    )
}

@Composable
private fun AuthMessage(message: String?) {
    if (message != null) {
        Text(
            text = message,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun LoadingButton(
    text: String,
    isLoading: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = !isLoading,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.height(22.dp),
                strokeWidth = 2.dp,
                color = Color.White
            )
        } else {
            Text(text)
        }
    }
}
