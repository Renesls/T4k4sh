package com.t4kash.app.ui.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.t4kash.app.ui.components.keepVisibleAboveKeyboard
import com.t4kash.app.ui.theme.T4Background
import com.t4kash.app.ui.theme.T4Border
import com.t4kash.app.ui.theme.T4Surface
import com.t4kash.app.ui.theme.T4Text
import com.t4kash.app.ui.viewmodel.AuthViewModel

@Composable
fun RegisterScreen(
    viewModel: AuthViewModel,
    onBack: () -> Unit,
    onVerificationRequired: (String) -> Unit
) {
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordConfirmation by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf<String?>(null) }
    var selectedUniversityId by rememberSaveable { mutableStateOf<Int?>(null) }
    var selectedCareerId by rememberSaveable { mutableStateOf<Int?>(null) }
    var studentCard by rememberSaveable { mutableStateOf("") }
    val focusManager = LocalFocusManager.current
    val uiState = viewModel.uiState
    val selectedUniversity = uiState.universities.firstOrNull {
        it.idUniversidad == selectedUniversityId
    }
    val selectedCareer = uiState.careers.firstOrNull {
        it.idCarrera == selectedCareerId
    }
    val universitySelectionText = when (selectedUniversityId) {
        null -> if (uiState.isLoadingOptions) {
            "Cargando universidades..."
        } else {
            "Seleccionar una opcion"
        }
        NO_INSTITUTION_ID -> NO_INSTITUTION_LABEL
        else -> selectedUniversity?.nombreUniversidad ?: "Seleccionar universidad"
    }

    LaunchedEffect(Unit) {
        viewModel.loadUniversities()
    }

    fun submit() {
        validationError = when {
            firstName.isBlank() || lastName.isBlank() || email.isBlank() ||
                password.isBlank() || passwordConfirmation.isBlank() ->
                "Completa todos los campos."

            selectedUniversityId == null ->
                "Indica si tienes correo institucional."

            selectedUniversityId != NO_INSTITUTION_ID && selectedCareerId == null ->
                "Selecciona tu carrera."

            selectedUniversity?.dominioCorreo.isNullOrBlank() &&
                selectedUniversityId != NO_INSTITUTION_ID &&
                studentCard.isBlank() ->
                "Ingresa tu numero de carnet para solicitar la validacion."

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
                universityId = selectedUniversityId.takeUnless {
                    it == NO_INSTITUTION_ID
                },
                careerId = selectedCareerId.takeIf {
                    selectedUniversityId != NO_INSTITUTION_ID
                },
                studentCard = studentCard.takeIf {
                    selectedUniversity?.dominioCorreo.isNullOrBlank()
                },
                onVerificationRequired = onVerificationRequired
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
                            text = "Una cuenta para participar a tu manera",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Con correo personal puedes publicar. Con correo universitario tambien puedes postularte.",
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
                            modifier = Modifier
                                .fillMaxWidth()
                                .keepVisibleAboveKeyboard(),
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
                            modifier = Modifier
                                .fillMaxWidth()
                                .keepVisibleAboveKeyboard(),
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
                            modifier = Modifier
                                .fillMaxWidth()
                                .keepVisibleAboveKeyboard(),
                            label = { Text("Correo") },
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Email,
                                imeAction = ImeAction.Next
                            )
                        )
                        SelectionMenu(
                            label = "Correo institucional",
                            value = universitySelectionText,
                            options = listOf(
                                NO_INSTITUTION_ID to NO_INSTITUTION_LABEL
                            ) + uiState.universities.map {
                                it.idUniversidad to it.nombreUniversidad
                            },
                            enabled = true,
                            onSelected = { universityId ->
                                selectedUniversityId = universityId
                                selectedCareerId = null
                                studentCard = ""
                                validationError = null
                                viewModel.clearError()
                                if (universityId != NO_INSTITUTION_ID) {
                                    viewModel.loadCareers(universityId)
                                }
                            }
                        )
                        if (
                            selectedUniversityId != null &&
                            selectedUniversityId != NO_INSTITUTION_ID
                        ) {
                            SelectionMenu(
                                label = "Carrera",
                                value = selectedCareer?.nombreCarrera
                                    ?: if (uiState.isLoadingOptions) {
                                        "Cargando carreras..."
                                    } else {
                                        "Seleccionar carrera"
                                    },
                                options = uiState.careers.map {
                                    it.idCarrera to it.nombreCarrera
                                },
                                enabled = !uiState.isLoadingOptions,
                                onSelected = { careerId ->
                                    selectedCareerId = careerId
                                    validationError = null
                                    viewModel.clearError()
                                }
                            )
                        }
                        if (
                            selectedUniversityId != null &&
                            selectedUniversityId != NO_INSTITUTION_ID &&
                            selectedUniversity?.dominioCorreo.isNullOrBlank()
                        ) {
                            Text(
                                text = "Esta universidad requiere validar carnet o constancia despues de confirmar el correo.",
                                style = MaterialTheme.typography.bodySmall,
                                color = T4Text
                            )
                            OutlinedTextField(
                                value = studentCard,
                                onValueChange = {
                                    studentCard = it.take(50)
                                    validationError = null
                                    viewModel.clearError()
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .keepVisibleAboveKeyboard(),
                                label = { Text("Numero de carnet") },
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                        OutlinedTextField(
                            value = password,
                            onValueChange = {
                                password = it
                                validationError = null
                                viewModel.clearError()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .keepVisibleAboveKeyboard(),
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
                            modifier = Modifier
                                .fillMaxWidth()
                                .keepVisibleAboveKeyboard(),
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

private const val NO_INSTITUTION_ID = 0
private const val NO_INSTITUTION_LABEL = "No tengo correo institucional"

@Composable
private fun SelectionMenu(
    label: String,
    value: String,
    options: List<Pair<Int, String>>,
    enabled: Boolean,
    onSelected: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = T4Text
        )
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = { expanded = true },
                enabled = enabled,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = value,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = Icons.Filled.ArrowDropDown,
                    contentDescription = null
                )
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                options.forEach { (id, name) ->
                    DropdownMenuItem(
                        text = { Text(name) },
                        onClick = {
                            expanded = false
                            onSelected(id)
                        }
                    )
                }
            }
        }
    }
}
