package com.t4kash.app.ui.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.t4kash.app.ui.components.ConnectionErrorState
import com.t4kash.app.ui.components.EmptyState
import com.t4kash.app.ui.components.StatusChip
import com.t4kash.app.ui.components.T4PatternSurface
import com.t4kash.app.ui.components.T4TopBar
import com.t4kash.app.ui.session.UserSession
import com.t4kash.app.ui.formatApiDateTime
import com.t4kash.app.ui.formatNioCurrency
import com.t4kash.app.ui.model.CategoryDto
import com.t4kash.app.ui.model.TaskDto
import com.t4kash.app.ui.parseApiDateTime
import com.t4kash.app.ui.theme.T4AmberContainer
import com.t4kash.app.ui.theme.T4Background
import com.t4kash.app.ui.theme.T4Border
import com.t4kash.app.ui.theme.T4Danger
import com.t4kash.app.ui.theme.T4Mint
import com.t4kash.app.ui.theme.T4MintDark
import com.t4kash.app.ui.theme.T4PrimaryContainer
import com.t4kash.app.ui.theme.T4PrimaryDark
import com.t4kash.app.ui.theme.T4Surface
import com.t4kash.app.ui.theme.T4Text
import com.t4kash.app.ui.theme.T4TextMuted
import com.t4kash.app.ui.viewmodel.MarketplaceViewModel
import java.util.concurrent.TimeUnit

@Composable
fun MyPublicationsScreen(
    initialFilter: String,
    viewModel: MarketplaceViewModel,
    onBack: () -> Unit,
    onTaskSelected: (Int) -> Unit,
    onEditTask: (Int) -> Unit
) {
    val state = viewModel.uiState
    var selectedFilterName by rememberSaveable(initialFilter) {
        mutableStateOf(PublicationFilter.fromRoute(initialFilter).name)
    }
    val selectedFilter = PublicationFilter.valueOf(selectedFilterName)
    val ownTasks = state.tasks
        .filter { it.idCliente == UserSession.requireUserId() }
        .sortedByDescending { it.fechaPublicacion }
    val filteredTasks = ownTasks.filter {
        selectedFilter.status == null ||
            it.estadoTarea.equals(selectedFilter.status, ignoreCase = true)
    }
    var taskToCancel by remember { mutableStateOf<TaskDto?>(null) }

    taskToCancel?.let { task ->
        AlertDialog(
            onDismissRequest = { taskToCancel = null },
            title = { Text("Cancelar publicacion") },
            text = {
                Text(
                    "La oportunidad dejara de aceptar postulaciones, pero permanecera en el historial."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.cancelTask(task.idTarea)
                        taskToCancel = null
                    }
                ) {
                    Text("Cancelar publicacion", color = T4Danger)
                }
            },
            dismissButton = {
                TextButton(onClick = { taskToCancel = null }) {
                    Text("Volver")
                }
            }
        )
    }

    LaunchedEffect(Unit) {
        viewModel.refresh()
    }

    Scaffold(
        containerColor = T4Background,
        topBar = {
            T4TopBar(
                title = "Mis publicaciones",
                subtitle = "Historial de oportunidades",
                onBack = onBack
            )
        }
    ) { innerPadding ->
        when {
            state.isLoading && state.tasks.isEmpty() -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.size(12.dp))
                    Text("Actualizando publicaciones...")
                }
            }

            state.errorMessage != null && state.tasks.isEmpty() -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    ConnectionErrorState(
                        message = state.errorMessage,
                        onRetry = { viewModel.refresh(force = true) }
                    )
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentPadding = PaddingValues(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        T4PatternSurface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "${ownTasks.size} oportunidades publicadas",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = publicationSummary(ownTasks),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White.copy(alpha = 0.82f)
                                )
                            }
                        }
                    }

                    item {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(PublicationFilter.entries) { filter ->
                                StatusChip(
                                    text = filter.label,
                                    selected = selectedFilter == filter,
                                    modifier = Modifier.clickable {
                                        selectedFilterName = filter.name
                                    }
                                )
                            }
                        }
                    }

                    state.errorMessage?.let { error ->
                        item {
                            Text(
                                text = error,
                                modifier = Modifier.padding(horizontal = 16.dp),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    state.taskMutationError?.let { error ->
                        item {
                            Text(
                                text = error,
                                modifier = Modifier.padding(horizontal = 16.dp),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }

                    if (filteredTasks.isEmpty()) {
                        item {
                            EmptyState(
                                title = if (ownTasks.isEmpty()) {
                                    "Aun no has publicado oportunidades"
                                } else {
                                    "No hay publicaciones en este estado"
                                },
                                message = if (ownTasks.isEmpty()) {
                                    "Las oportunidades creadas desde Post apareceran aqui."
                                } else {
                                    "Selecciona otro filtro para consultar el historial."
                                },
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    } else {
                        items(
                            items = filteredTasks,
                            key = { it.idTarea }
                        ) { task ->
                            PublicationCard(
                                task = task,
                                category = state.categories.firstOrNull {
                                    it.idCategoria == task.idCategoria
                                },
                                onClick = { onTaskSelected(task.idTarea) },
                                onEdit = { onEditTask(task.idTarea) },
                                onCancel = { taskToCancel = task },
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PublicationCard(
    task: TaskDto,
    category: CategoryDto?,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val statusColors = publicationStatusColors(task.estadoTarea)
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = T4Surface),
        border = BorderStroke(1.dp, T4Border.copy(alpha = 0.65f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = task.titulo,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = T4Text,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = category?.nombreCategoria ?: "Categoria #${task.idCategoria}",
                        style = MaterialTheme.typography.bodySmall,
                        color = T4TextMuted
                    )
                }
                StatusChip(
                    text = task.estadoTarea.lowercase()
                        .replaceFirstChar { it.uppercase() },
                    containerColor = statusColors.first,
                    contentColor = statusColors.second
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                PublicationValue(
                    icon = Icons.Filled.Payments,
                    label = "Presupuesto",
                    value = formatNioCurrency(task.presupuesto),
                    modifier = Modifier.weight(1f)
                )
                PublicationValue(
                    icon = Icons.Filled.Place,
                    label = "Modalidad",
                    value = task.modalidad ?: "No definida",
                    modifier = Modifier.weight(1f)
                )
            }

            PublicationDateRow(
                icon = Icons.Filled.CalendarMonth,
                label = "Publicada",
                value = formatApiDateTime(task.fechaPublicacion)
            )
            PublicationDateRow(
                icon = Icons.Filled.EventAvailable,
                label = "Cierre de postulaciones",
                value = formatApiDateTime(task.fechaLimitePostulacion)
            )
            PublicationDateRow(
                icon = Icons.Filled.Work,
                label = "Fecha limite del trabajo",
                value = formatApiDateTime(task.fechaLimite)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = task.deadlineSummary(),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = statusColors.second
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Ver detalle",
                        style = MaterialTheme.typography.labelLarge,
                        color = T4TextMuted
                    )
                    Spacer(modifier = Modifier.size(4.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = T4TextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            if (task.estadoTarea.equals("PUBLICADA", ignoreCase = true)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onEdit,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.size(6.dp))
                        Text("Editar")
                    }
                    OutlinedButton(
                        onClick = onCancel,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.DeleteOutline,
                            contentDescription = null,
                            tint = T4Danger
                        )
                        Spacer(modifier = Modifier.size(6.dp))
                        Text("Cancelar", color = T4Danger)
                    }
                }
            }
        }
    }
}

@Composable
private fun PublicationValue(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = T4TextMuted,
            modifier = Modifier.size(20.dp)
        )
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = T4TextMuted
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = T4Text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun PublicationDateRow(
    icon: ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = T4TextMuted,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodySmall,
            color = T4TextMuted
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = T4Text
        )
    }
}

private fun publicationSummary(tasks: List<TaskDto>): String {
    val active = tasks.count { it.estadoTarea.equals("PUBLICADA", ignoreCase = true) }
    val assigned = tasks.count { it.estadoTarea.equals("ASIGNADA", ignoreCase = true) }
    val closed = tasks.count { it.estadoTarea.equals("CERRADA", ignoreCase = true) }
    return "$active activas · $assigned asignadas · $closed cerradas"
}

private fun publicationStatusColors(status: String): Pair<Color, Color> {
    return when (status.uppercase()) {
        "PUBLICADA" -> T4Mint to T4MintDark
        "ASIGNADA" -> T4PrimaryContainer to T4PrimaryDark
        "CERRADA" -> Color(0xFFFFE4E4) to T4Danger
        else -> T4AmberContainer to T4Text
    }
}

private fun TaskDto.deadlineSummary(): String {
    if (estadoTarea.equals("ASIGNADA", ignoreCase = true)) {
        return "Trabajo asignado"
    }
    if (estadoTarea.equals("CERRADA", ignoreCase = true)) {
        return "Postulaciones cerradas"
    }
    val deadline = parseApiDateTime(fechaLimitePostulacion ?: fechaLimite)
        ?: return "Sin cierre programado"
    val remainingMillis = deadline.time - System.currentTimeMillis()
    if (remainingMillis <= 0) {
        return "Plazo finalizado"
    }
    val days = TimeUnit.MILLISECONDS.toDays(remainingMillis)
    if (days > 0) {
        return if (days == 1L) "Finaliza en 1 dia" else "Finaliza en $days dias"
    }
    val hours = TimeUnit.MILLISECONDS.toHours(remainingMillis).coerceAtLeast(1)
    return if (hours == 1L) "Finaliza en 1 hora" else "Finaliza en $hours horas"
}

private enum class PublicationFilter(
    val label: String,
    val status: String?
) {
    ALL("Todas", null),
    ACTIVE("Activas", "PUBLICADA"),
    ASSIGNED("Asignadas", "ASIGNADA"),
    CLOSED("Cerradas", "CERRADA");

    companion object {
        fun fromRoute(route: String): PublicationFilter {
            return entries.firstOrNull {
                it.status.equals(route, ignoreCase = true) ||
                    it.name.equals(route, ignoreCase = true)
            } ?: ALL
        }
    }
}
