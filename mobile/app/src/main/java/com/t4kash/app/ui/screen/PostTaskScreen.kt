package com.t4kash.app.ui.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TipsAndUpdates
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.t4kash.app.ui.components.StatusChip
import com.t4kash.app.ui.components.T4BottomBar
import com.t4kash.app.ui.components.T4TopBar
import com.t4kash.app.ui.navigation.Routes
import com.t4kash.app.ui.theme.T4Background
import com.t4kash.app.ui.theme.T4Border
import com.t4kash.app.ui.theme.T4MintDark
import com.t4kash.app.ui.theme.T4Primary
import com.t4kash.app.ui.theme.T4PrimarySoft
import com.t4kash.app.ui.theme.T4Surface
import com.t4kash.app.ui.theme.T4Text
import com.t4kash.app.ui.theme.T4TextMuted

@Composable
fun PostTaskScreen(
    currentRoute: String = Routes.POST,
    onNavigate: (String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var budget by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }

    fun validate() {
        successMessage = null
        errorMessage = when {
            title.isBlank() -> "Escribe un título para la oportunidad."
            description.length < 20 -> "Agrega una descripción de al menos 20 caracteres."
            budget.toDoubleOrNull() == null -> "Ingresa un presupuesto numérico válido."
            else -> null
        }
        if (errorMessage == null) {
            successMessage = "Formulario validado. Después lo conectamos al endpoint POST /tasks."
        }
    }

    Scaffold(
        containerColor = T4Background,
        topBar = {
            T4TopBar(
                title = "Publicar",
                subtitle = "Crea una nueva oportunidad"
            )
        },
        bottomBar = {
            T4BottomBar(
                currentRoute = currentRoute,
                onNavigate = onNavigate
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(T4Background, Color(0xFFF4F7FF))
                    )
                ),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(T4PrimarySoft, T4Primary)
                            ),
                            RoundedCornerShape(24.dp)
                        )
                        .padding(20.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            StatusChip(
                                text = "Publicación",
                                selected = true,
                                containerColor = Color.White.copy(alpha = 0.18f),
                                contentColor = Color.White
                            )
                            StatusChip(
                                text = "MVP",
                                selected = true,
                                containerColor = Color.White.copy(alpha = 0.14f),
                                contentColor = Color.White
                            )
                        }
                        Text(
                            text = "Publica una oportunidad clara y atractiva.",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Ordena el título, la descripción y el presupuesto para que los estudiantes entiendan rápido lo que necesitan resolver.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.84f)
                        )
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = T4Surface),
                    shape = RoundedCornerShape(22.dp),
                    border = BorderStroke(1.dp, T4Border.copy(alpha = 0.60f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "Datos de la oportunidad",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = T4Text
                        )
                        Text(
                            text = "Estos campos son la base del flujo de publicación.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = T4TextMuted
                        )

                        OutlinedTextField(
                            value = title,
                            onValueChange = {
                                title = it
                                errorMessage = null
                                successMessage = null
                            },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Título") },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp)
                        )
                        OutlinedTextField(
                            value = description,
                            onValueChange = {
                                description = it
                                errorMessage = null
                                successMessage = null
                            },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Descripción") },
                            minLines = 4,
                            shape = RoundedCornerShape(16.dp)
                        )
                        OutlinedTextField(
                            value = budget,
                            onValueChange = {
                                budget = it
                                errorMessage = null
                                successMessage = null
                            },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Presupuesto") },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )

                        if (errorMessage != null) {
                            Text(
                                text = errorMessage.orEmpty(),
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        if (successMessage != null) {
                            Text(
                                text = successMessage.orEmpty(),
                                color = T4MintDark,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        Button(
                            onClick = ::validate,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Validar publicación")
                        }
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = T4Surface),
                    shape = RoundedCornerShape(22.dp),
                    border = BorderStroke(1.dp, T4Border.copy(alpha = 0.50f))
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.TipsAndUpdates,
                                contentDescription = null,
                                tint = T4Primary
                            )
                            Text(
                                text = "Consejo rápido",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = T4Text
                            )
                        }
                        Text(
                            text = "Un buen título y un presupuesto claro hacen que la publicación se vea más profesional y suba la confianza del usuario.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = T4TextMuted
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            StatusChip(text = "Claro")
                            StatusChip(text = "Corto")
                            StatusChip(text = "Verificable")
                        }
                    }
                }
            }
        }
    }
}
