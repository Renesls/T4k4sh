package com.t4kash.app.ui.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.t4kash.app.ui.components.ConnectionErrorState
import com.t4kash.app.ui.components.EmptyState
import com.t4kash.app.ui.components.StatusChip
import com.t4kash.app.ui.components.T4TopBar
import com.t4kash.app.ui.model.TaskDto
import com.t4kash.app.ui.theme.T4Background
import com.t4kash.app.ui.theme.T4Border
import com.t4kash.app.ui.theme.T4MintDark
import com.t4kash.app.ui.theme.T4Primary
import com.t4kash.app.ui.theme.T4PrimarySoft
import com.t4kash.app.ui.theme.T4Surface
import com.t4kash.app.ui.theme.T4Text
import com.t4kash.app.ui.theme.T4TextMuted
import com.t4kash.app.ui.viewmodel.MarketplaceViewModel

@Composable
fun OpportunityDetailScreen(
    taskId: Int,
    viewModel: MarketplaceViewModel,
    onBack: () -> Unit,
    onApply: () -> Unit
) {
    val state = viewModel.uiState
    val task = state.tasks.firstOrNull { it.idTarea == taskId }

    Scaffold(
        containerColor = T4Background,
        topBar = {
            T4TopBar(
                title = "Detalle",
                subtitle = "Oportunidad universitaria",
                onBack = onBack
            )
        },
        bottomBar = {
            if (task != null) {
                DetailActionBar(onApply = onApply)
            }
        }
    ) { innerPadding ->
        when {
            state.isLoading -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Cargando detalle...")
                }
            }

            state.errorMessage != null -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    ConnectionErrorState(
                        message = state.errorMessage,
                        onRetry = viewModel::refresh
                    )
                }
            }

            task == null -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    EmptyState(
                        title = "Oportunidad no encontrada",
                        message = "La tarea seleccionada ya no esta disponible o no se pudo sincronizar."
                    )
                }
            }

            else -> {
                OpportunityDetailContent(
                    task = task,
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }
    }
}

@Composable
private fun OpportunityDetailContent(
    task: TaskDto,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(T4Background, Color(0xFFF2F2ED))
                )
            ),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { HeroCard(task = task) }
        item {
            DetailSection(
                title = "Descripcion",
                icon = Icons.Filled.Description
            ) {
                Text(
                    text = task.descripcion,
                    color = T4TextMuted,
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Esta oportunidad permite que estudiantes conecten con trabajos academicos y proyectos flexibles dentro de la comunidad universitaria.",
                    color = T4TextMuted,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        item {
            DetailSection(
                title = "Perfil ideal",
                icon = Icons.Filled.School
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatusChip(text = task.tipoOportunidad)
                    StatusChip(text = task.modalidad ?: "Campus")
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatusChip(text = "Comunicacion")
                    StatusChip(text = "Responsabilidad")
                }
            }
        }
        item {
            DetailSection(title = "Resumen rapido") {
                SummaryRow("Estado", task.estadoTarea, Icons.Filled.Event)
                SummaryRow("Modalidad", task.modalidad ?: "No definida", Icons.Filled.Place)
                SummaryRow("Fecha limite", task.fechaLimite ?: "Por confirmar", Icons.Filled.Event)
                SummaryRow("Visibilidad", task.visibilidad, Icons.Filled.Place)
            }
        }
    }
}

@Composable
private fun HeroCard(task: TaskDto) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = T4Surface),
        border = BorderStroke(1.dp, T4Border.copy(alpha = 0.55f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(T4PrimarySoft, T4Primary)
                    )
                )
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatusChip(
                    text = task.tipoOportunidad,
                    selected = true,
                    containerColor = Color.White.copy(alpha = 0.18f),
                    contentColor = Color.White
                )
                Text(
                    text = task.estadoTarea,
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.86f)
                )
            }

            Text(
                text = task.titulo,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = "$${"%.2f".format(task.presupuesto)}",
                    color = Color.White,
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Black
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "presupuesto estimado",
                    color = Color.White.copy(alpha = 0.76f),
                    style = MaterialTheme.typography.labelMedium
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.School,
                    contentDescription = null,
                    tint = Color.White
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Cliente #${task.idCliente}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                    Text(
                        text = "Perfil verificado por T4KASH",
                        color = Color.White.copy(alpha = 0.76f),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailSection(
    title: String,
    icon: ImageVector? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = T4Surface),
        border = BorderStroke(1.dp, T4Border.copy(alpha = 0.55f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = T4Primary
                    )
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = T4Text
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            content()
        }
    }
}

@Composable
private fun SummaryRow(
    label: String,
    value: String,
    icon: ImageVector
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = T4Primary
        )
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = T4Text
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = T4TextMuted
            )
        }
    }
}

@Composable
private fun DetailActionBar(
    onApply: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(T4Surface)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedButton(
            onClick = {},
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = Icons.Filled.BookmarkBorder,
                contentDescription = null
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text("Guardar")
        }
        Button(
            onClick = onApply,
            modifier = Modifier.weight(2f)
        ) {
            Text("Postularse")
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null
            )
        }
    }
}
