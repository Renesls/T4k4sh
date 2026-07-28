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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.t4kash.app.ui.components.T4PatternSurface
import com.t4kash.app.ui.components.T4TopBar
import com.t4kash.app.ui.theme.T4Background
import com.t4kash.app.ui.theme.T4Border
import com.t4kash.app.ui.theme.T4Surface
import com.t4kash.app.ui.theme.T4Text
import com.t4kash.app.ui.viewmodel.AuthViewModel

@Composable
fun RegisterScreen(
    viewModel: AuthViewModel,
    onBack: () -> Unit,
    onRegisterSuccess: () -> Unit
) {
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordConfirmation by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf<String?>(null) }
    val focusManager = LocalFocusManager.current
    val uiState = viewModel.uiState

    fun submit() {
        validationError = when {
            firstName.isBlank() || lastName.isBlank() || email.isBlank() ||
                password.isBlank() || passwordConfirmation.isBlank() ->
                "Completa todos los campos."

            !email.contains("@") ->
                "Ingresa un correo válido."

            password.length < 8 ->
                "La contraseña debe tener al menos 8 caracteres."

            password != passwordConfirmation ->
                "Las contraseñas no coinciden."

            else -> null
        }
        if (validationError == null) {
            focusManager.clearFocus()
            viewModel.register(
                firstName = firstName,
                lastName = lastName,
                email = email,
                password = password,
                onSuccess = onRegisterSuccess
            )
        }
    }

    Scaffold(
        containerColor = T4Background,
        topBar = {
            T4TopBar(
                title = "Crear cuenta",
                subtitle = "Identidad y acceso",
                onBack = onBack
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                T4PatternSurface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Una cuenta, dos formas de participar",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Publica oportunidades y postúlate con el mismo perfil.",
                            style = MaterialTheme.typography.bodyMedium,
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
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Datos de la cuenta",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = T4Text
                        )
                        OutlinedTextField(
                            value = firstName,
                            onValueChange = {
                                firstName = it
                                validationError = null
                                viewModel.clearError()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Nombre") },
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp)
                        )
                        OutlinedTextField(
                            value = lastName,
                            onValueChange = {
                                lastName = it
                                validationError = null
                                viewModel.clearError()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Apellido") },
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp)
                        )
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
                            shape = RoundedCornerShape(8.dp),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Email,
                                imeAction = ImeAction.Next
                            )
                        )
                        OutlinedTextField(
                            value = password,
                            onValueChange = {
                                password = it
                                validationError = null
                                viewModel.clearError()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Contraseña") },
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp),
                            visualTransformation = if (passwordVisible) {
                                VisualTransformation.None
                            } else {
                                PasswordVisualTransformation()
                            },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Next
                            ),
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        imageVector = if (passwordVisible) {
                                            Icons.Filled.VisibilityOff
                                        } else {
                                            Icons.Filled.Visibility
                                        },
                                        contentDescription = if (passwordVisible) {
                                            "Ocultar contraseña"
                                        } else {
                                            "Mostrar contraseña"
                                        }
                                    )
                                }
                            }
                        )
                        OutlinedTextField(
                            value = passwordConfirmation,
                            onValueChange = {
                                passwordConfirmation = it
                                validationError = null
                                viewModel.clearError()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Confirmar contraseña") },
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp),
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(onDone = { submit() })
                        )

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
                                .height(52.dp)
                        ) {
                            if (uiState.isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.height(22.dp),
                                    strokeWidth = 2.dp,
                                    color = Color.White
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Filled.PersonAdd,
                                    contentDescription = null
                                )
                                Text(
                                    text = "Crear cuenta",
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
