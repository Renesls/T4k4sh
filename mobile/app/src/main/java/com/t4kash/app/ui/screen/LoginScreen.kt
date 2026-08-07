package com.t4kash.app.ui.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.t4kash.app.R
import com.t4kash.app.ui.components.T4LightPatternHeader
import com.t4kash.app.ui.components.keepVisibleAboveKeyboard
import com.t4kash.app.ui.theme.T4Background
import com.t4kash.app.ui.theme.T4Mint
import com.t4kash.app.ui.theme.T4Primary
import com.t4kash.app.ui.theme.T4SurfaceVariant
import com.t4kash.app.ui.theme.T4Text
import com.t4kash.app.ui.theme.T4TextMuted
import com.t4kash.app.ui.viewmodel.AuthViewModel

@Composable
fun LoginScreen(
    viewModel: AuthViewModel,
    onLoginVerification: (String) -> Unit,
    onRegister: () -> Unit,
    onVerifyEmail: () -> Unit,
    onForgotPassword: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf<String?>(null) }
    val focusManager = LocalFocusManager.current
    val uiState = viewModel.uiState

    fun submit() {
        validationError = when {
            email.isBlank() || password.isBlank() ->
                "Completa tu correo y contrasena para continuar."

            !email.contains("@") -> "Ingresa un correo valido."
            else -> null
        }
        if (validationError == null) {
            focusManager.clearFocus()
            viewModel.login(email, password, onLoginVerification)
        }
    }

    Scaffold(containerColor = T4Background) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
                .verticalScroll(rememberScrollState())
        ) {
            T4LightPatternHeader(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
            ) {
                Surface(
                    modifier = Modifier
                        .size(88.dp)
                        .align(Alignment.Center),
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    shadowElevation = 8.dp
                ) {
                    Image(
                        painter = painterResource(R.drawable.t4kash_logo),
                        contentDescription = "Logotipo de T4KASH",
                        contentScale = ContentScale.Crop
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 22.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Bienvenido de vuelta",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = T4Text
                )
                Text(
                    text = "Inicia sesion para encontrar tu proxima oportunidad.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = T4TextMuted
                )
                Spacer(modifier = Modifier.height(4.dp))

                LoginField(
                    value = email,
                    onValueChange = {
                        email = it
                        validationError = null
                        viewModel.clearError()
                    },
                    label = "Correo electronico",
                    icon = {
                        Icon(Icons.Filled.Email, contentDescription = null)
                    },
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next,
                    isError = validationError != null
                )
                LoginField(
                    value = password,
                    onValueChange = {
                        password = it
                        validationError = null
                        viewModel.clearError()
                    },
                    label = "Contrasena",
                    icon = {
                        Icon(Icons.Filled.Lock, contentDescription = null)
                    },
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done,
                    keyboardActions = KeyboardActions(onDone = { submit() }),
                    visualTransformation = if (passwordVisible) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) {
                                    Icons.Filled.VisibilityOff
                                } else {
                                    Icons.Filled.Visibility
                                },
                                contentDescription = if (passwordVisible) {
                                    "Ocultar contrasena"
                                } else {
                                    "Mostrar contrasena"
                                }
                            )
                        }
                    },
                    isError = validationError != null
                )

                TextButton(
                    onClick = {
                        viewModel.clearError()
                        onForgotPassword()
                    },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Olvidaste tu contrasena?")
                }

                val error = validationError ?: uiState.errorMessage
                if (error != null) {
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Button(
                    onClick = ::submit,
                    enabled = !uiState.isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = T4Mint,
                        contentColor = T4Text
                    )
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.dp,
                            color = T4Text
                        )
                    } else {
                        Text("Iniciar sesion", fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "No tienes una cuenta?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = T4TextMuted
                    )
                    TextButton(
                        onClick = {
                            viewModel.clearError()
                            onRegister()
                        }
                    ) {
                        Text("Registrate gratis", fontWeight = FontWeight.Bold)
                    }
                }
                TextButton(
                    onClick = {
                        viewModel.clearError()
                        onVerifyEmail()
                    },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text("Ya tengo un codigo de verificacion")
                }
            }
        }
    }
}

@Composable
private fun LoginField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: @Composable () -> Unit,
    keyboardType: KeyboardType,
    imeAction: ImeAction,
    isError: Boolean,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailingIcon: (@Composable () -> Unit)? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .fillMaxWidth()
            .keepVisibleAboveKeyboard(),
        label = { Text(label) },
        leadingIcon = icon,
        trailingIcon = trailingIcon,
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = T4SurfaceVariant,
            unfocusedContainerColor = T4SurfaceVariant,
            focusedBorderColor = T4Primary,
            unfocusedBorderColor = Color.Transparent
        ),
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType,
            imeAction = imeAction
        ),
        keyboardActions = keyboardActions,
        visualTransformation = visualTransformation,
        isError = isError
    )
}
