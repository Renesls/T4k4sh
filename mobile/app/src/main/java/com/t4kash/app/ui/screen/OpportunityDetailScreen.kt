package com.t4kash.app.ui.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.t4kash.app.ui.components.ConnectionErrorState
import com.t4kash.app.ui.components.EmptyState
import com.t4kash.app.ui.components.StatusChip
import com.t4kash.app.ui.components.T4PatternSurface
import com.t4kash.app.ui.components.T4TopBar
import com.t4kash.app.ui.components.t4CategoryColors
import com.t4kash.app.ui.session.UserSession
import com.t4kash.app.ui.formatApiDateTime
import com.t4kash.app.ui.formatNioCurrency
import com.t4kash.app.ui.model.CreateApplicationRequest
import com.t4kash.app.ui.model.TaskDto
import com.t4kash.app.ui.theme.T4Background
import com.t4kash.app.ui.theme.T4Border
import com.t4kash.app.ui.theme.T4Mint
import com.t4kash.app.ui.theme.T4Primary
import com.t4kash.app.ui.theme.T4Surface
import com.t4kash.app.ui.theme.T4Text
import com.t4kash.app.ui.theme.T4TextMuted
import com.t4kash.app.ui.viewmodel.MarketplaceViewModel

@Composable
fun OpportunityDetailScreen(
    taskId: Int,
    viewModel: MarketplaceViewModel,
    onBack: () -> Unit,
    onApply: () -> Unit,
    onOpenMap: () -> Unit,
    onManageApplications: () -> Unit
) {
    val state = viewModel.uiState
    val task = state.tasks.firstOrNull { it.idTarea == taskId }
    var showApplicationDialog by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(taskId) {
        viewModel.loadTaskAttachments(taskId)
    }

    LaunchedEffect(state.sentApplication?.idPostulacion) {
        if (state.sentApplication != null) {
            showApplicationDialog = false
            viewModel.clearApplicationFeedback()
            onApply()
        }
    }

    if (showApplicationDialog && task != null) {
        ApplicationDialog(
            task = task,
            isSubmitting = state.isApplying,
            apiError = state.applicationError,
            onDismiss = {
                if (!state.isApplying) {
                    showApplicationDialog = false
                    viewModel.clearApplicationFeedback()
                }
            },
            onSubmit = { message, proposedPrice ->
                viewModel.applyToTask(
                    taskId = task.idTarea,
                    request = CreateApplicationRequest(
                        idEstudiante = UserSession.requireUserId(),
                        mensaje = message,
                        precioPropuesto = proposedPrice
                    )
                )
            }
        )
    }

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
                DetailActionBar(
                    onApply = {
                        viewModel.clearApplicationFeedback()
                        showApplicationDialog = true
                    },
                    onManageApplications = onManageApplications,
                    isApplying = state.isApplying,
                    canApply = task.estadoTarea.equals("PUBLICADA", ignoreCase = true),
                    isOwnedTask = task.idCliente == UserSession.requireUserId()
                )
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
                    attachments = state.taskAttachments,
                    isLoadingAttachments = state.isLoadingTaskAttachments,
                    attachmentsError = state.taskAttachmentsError,
                    onOpenMap = onOpenMap,
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }
    }
}
@Composable
private fun OpportunityDetailContent(
    task: TaskDto,
    attachments: List<com.t4kash.app.ui.model.AttachmentDto>,
    isLoadingAttachments: Boolean,
    attachmentsError: String?,
    onOpenMap: () -> Unit,
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
        if (attachments.isNotEmpty() || isLoadingAttachments || attachmentsError != null) {
            item {
                DetailSection(
                    title = "Archivos adjuntos",
                    icon = Icons.Filled.Description
                ) {
                    if (isLoadingAttachments) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp))
                            Text(
                                text = "Cargando archivos...",
                                color = T4TextMuted
                            )
                        }
                    }
                    attachments.forEach { attachment ->
                        StoredAttachmentRow(attachment = attachment)
                    }
                    attachmentsError?.let { error ->
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
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
                SummaryRow(
                    "Fecha limite",
                    formatApiDateTime(task.fechaLimite, emptyValue = "Por confirmar"),
                    Icons.Filled.Event
                )
                SummaryRow("Visibilidad", task.visibilidad, Icons.Filled.Place)
                if (task.hasMapLocation()) {
                    SummaryRow(
                        label = "Ubicacion",
                        value = task.direccionReferencia ?: "Ver oportunidad en el mapa",
                        icon = Icons.Filled.Place,
                        onClick = onOpenMap
                    )
                }
            }
        }
    }
}

@Composable
private fun HeroCard(task: TaskDto) {
    val categoryColors = t4CategoryColors(task.idCategoria)
    T4PatternSurface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
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
                    containerColor = categoryColors.container,
                    contentColor = categoryColors.content
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
                    text = formatNioCurrency(task.presupuesto),
                    color = T4Mint,
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
        shape = RoundedCornerShape(8.dp),
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
    icon: ImageVector,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier
                }
            )
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = T4Primary
        )
        Column(modifier = Modifier.weight(1f)) {
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
        if (onClick != null) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Abrir en el mapa",
                tint = T4Primary
            )
        }
    }
}

private fun TaskDto.hasMapLocation(): Boolean {
    return latitud != null &&
        longitud != null &&
        !modalidad.equals("REMOTA", ignoreCase = true)
}

@Composable
private fun ApplicationDialog(
    task: TaskDto,
    isSubmitting: Boolean,
    apiError: String?,
    onDismiss: () -> Unit,
    onSubmit: (String?, Double?) -> Unit
) {
    var message by rememberSaveable(task.idTarea) { mutableStateOf("") }
    var proposedPrice by rememberSaveable(task.idTarea) {
        mutableStateOf(task.presupuesto.toString())
    }
    var validationError by rememberSaveable(task.idTarea) {
        mutableStateOf<String?>(null)
    }

    AlertDialog(
        modifier = Modifier.imePadding(),
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Enviar postulacion",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = task.titulo,
                    style = MaterialTheme.typography.titleMedium,
                    color = T4Text
                )
                OutlinedTextField(
                    value = message,
                    onValueChange = {
                        if (it.length <= 500) {
                            message = it
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Mensaje opcional") },
                    minLines = 3,
                    maxLines = 5,
                    enabled = !isSubmitting,
                    supportingText = {
                        Text("${message.length}/500")
                    }
                )
                OutlinedTextField(
                    value = proposedPrice,
                    onValueChange = {
                        proposedPrice = it
                        validationError = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Precio propuesto") },
                    prefix = { Text("C\$") },
                    singleLine = true,
                    enabled = !isSubmitting,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal
                    ),
                    isError = validationError != null
                )
                val error = validationError ?: apiError
                if (error != null) {
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsedPrice = proposedPrice
                        .trim()
                        .replace(',', '.')
                        .toDoubleOrNull()
                    if (parsedPrice == null || parsedPrice < 0) {
                        validationError = "Ingresa un precio valido."
                    } else {
                        onSubmit(
                            message.trim().takeIf { it.isNotEmpty() },
                            parsedPrice
                        )
                    }
                },
                enabled = !isSubmitting
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Enviar")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isSubmitting
            ) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
private fun DetailActionBar(
    onApply: () -> Unit,
    onManageApplications: () -> Unit,
    isApplying: Boolean,
    canApply: Boolean,
    isOwnedTask: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(T4Surface)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Button(
            onClick = if (isOwnedTask) onManageApplications else onApply,
            modifier = Modifier.fillMaxWidth(),
            enabled = isOwnedTask || (canApply && !isApplying)
        ) {
            if (isApplying && !isOwnedTask) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp
                )
            } else if (isOwnedTask) {
                Icon(
                    imageVector = Icons.Filled.Group,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Gestionar postulaciones")
            } else {
                Text(if (canApply) "Postularse" else "Postulaciones cerradas")
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null
                )
            }
        }
    }
}
