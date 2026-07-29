package com.t4kash.app.ui.screen

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.t4kash.app.ui.components.ConnectionErrorState
import com.t4kash.app.ui.components.EmptyState
import com.t4kash.app.ui.components.StatusChip
import com.t4kash.app.ui.components.T4PatternSurface
import com.t4kash.app.ui.components.T4TopBar
import com.t4kash.app.ui.model.AdminSummaryDto
import com.t4kash.app.ui.model.StudentVerificationDto
import com.t4kash.app.ui.model.TaskDto
import com.t4kash.app.ui.theme.T4Background
import com.t4kash.app.ui.theme.T4Border
import com.t4kash.app.ui.theme.T4Mint
import com.t4kash.app.ui.theme.T4MintDark
import com.t4kash.app.ui.theme.T4Primary
import com.t4kash.app.ui.theme.T4Surface
import com.t4kash.app.ui.theme.T4Text
import com.t4kash.app.ui.theme.T4TextMuted
import com.t4kash.app.ui.viewmodel.MarketplaceViewModel
import java.text.NumberFormat
import java.util.Locale

@Composable
fun AdminScreen(
    viewModel: MarketplaceViewModel,
    onBack: () -> Unit
) {
    val state = viewModel.uiState
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedTab by remember { mutableIntStateOf(0) }
    var approvalTarget by remember { mutableStateOf<StudentVerificationDto?>(null) }
    var rejectionTarget by remember { mutableStateOf<StudentVerificationDto?>(null) }
    var cancellationTarget by remember { mutableStateOf<TaskDto?>(null) }

    LaunchedEffect(Unit) {
        viewModel.loadAdminDashboard()
    }
    LaunchedEffect(state.adminMessage, state.adminError, state.adminSummary) {
        val message = state.adminMessage
            ?: state.adminError?.takeIf { state.adminSummary != null }
        if (message != null) {
            snackbarHostState.showSnackbar(message)
            viewModel.clearAdminFeedback()
        }
    }

    approvalTarget?.let { verification ->
        AlertDialog(
            onDismissRequest = { approvalTarget = null },
            title = { Text("Aprobar perfil estudiantil") },
            text = {
                Text("Se habilitara el rol ESTUDIANTE para ${verification.correo}.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.approveStudentVerification(verification.idUsuario)
                        approvalTarget = null
                    }
                ) {
                    Text("Aprobar")
                }
            },
            dismissButton = {
                TextButton(onClick = { approvalTarget = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
    rejectionTarget?.let { verification ->
        RejectVerificationDialog(
            email = verification.correo,
            onDismiss = { rejectionTarget = null },
            onConfirm = { observation ->
                viewModel.rejectStudentVerification(
                    verification.idUsuario,
                    observation
                )
                rejectionTarget = null
            }
        )
    }
    cancellationTarget?.let { task ->
        AlertDialog(
            onDismissRequest = { cancellationTarget = null },
            title = { Text("Retirar publicacion") },
            text = {
                Text(
                    "La oportunidad \"${task.titulo}\" dejara de mostrarse y sus " +
                        "postulaciones pendientes se cancelaran."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.cancelTaskAsAdmin(task.idTarea)
                        cancellationTarget = null
                    }
                ) {
                    Text("Retirar")
                }
            },
            dismissButton = {
                TextButton(onClick = { cancellationTarget = null }) {
                    Text("Conservar")
                }
            }
        )
    }

    Scaffold(
        containerColor = T4Background,
        topBar = {
            T4TopBar(
                title = "Administracion",
                subtitle = "Moderacion y validaciones",
                onBack = onBack
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                AdminHero(onRefresh = { viewModel.loadAdminDashboard(force = true) })
            }

            if (state.isLoadingAdmin && state.adminSummary == null) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator()
                        Text("Cargando panel...")
                    }
                }
            } else if (state.adminError != null && state.adminSummary == null) {
                item {
                    ConnectionErrorState(
                        message = state.adminError,
                        onRetry = { viewModel.loadAdminDashboard(force = true) }
                    )
                }
            } else {
                state.adminSummary?.let { summary ->
                    item { AdminSummary(summary) }
                }
                item {
                    PrimaryTabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = T4Background,
                        contentColor = T4Primary
                    ) {
                        listOf(
                            "Validaciones (${state.pendingStudentVerifications.size})",
                            "Publicaciones"
                        ).forEachIndexed { index, label ->
                            Tab(
                                selected = selectedTab == index,
                                onClick = { selectedTab = index },
                                text = { Text(label, maxLines = 1) }
                            )
                        }
                    }
                }

                if (selectedTab == 0) {
                    if (state.pendingStudentVerifications.isEmpty()) {
                        item {
                            EmptyState(
                                title = "Todo revisado",
                                message = "No hay perfiles estudiantiles pendientes."
                            )
                        }
                    } else {
                        items(
                            items = state.pendingStudentVerifications,
                            key = { it.idVerificacion }
                        ) { verification ->
                            VerificationAdminCard(
                                verification = verification,
                                isWorking =
                                    state.adminActionKey ==
                                        "verification:${verification.idUsuario}",
                                onApprove = { approvalTarget = verification },
                                onReject = { rejectionTarget = verification }
                            )
                        }
                    }
                } else {
                    items(
                        items = state.adminTasks,
                        key = { it.idTarea }
                    ) { task ->
                        AdminTaskCard(
                            task = task,
                            isWorking = state.adminActionKey == "task:${task.idTarea}",
                            onCancel = { cancellationTarget = task }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminHero(onRefresh: () -> Unit) {
    T4PatternSurface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.AdminPanelSettings,
                contentDescription = null,
                tint = T4Mint,
                modifier = Modifier.size(38.dp)
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Centro de control",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Revisa identidades y modera el marketplace.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
            TextButton(onClick = onRefresh) {
                Icon(
                    imageVector = Icons.Filled.Refresh,
                    contentDescription = "Actualizar",
                    tint = T4Mint
                )
            }
        }
    }
}

@Composable
private fun AdminSummary(summary: AdminSummaryDto) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AdminMetric(
                label = "Usuarios",
                value = summary.usuarios,
                icon = Icons.Filled.People,
                modifier = Modifier.weight(1f)
            )
            AdminMetric(
                label = "Por revisar",
                value = summary.verificacionesPendientes,
                icon = Icons.Filled.Description,
                modifier = Modifier.weight(1f)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AdminMetric(
                label = "Publicadas",
                value = summary.publicacionesActivas,
                icon = Icons.Filled.TaskAlt,
                modifier = Modifier.weight(1f)
            )
            AdminMetric(
                label = "Asignadas",
                value = summary.trabajosAsignados,
                icon = Icons.Filled.CheckCircle,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun AdminMetric(
    label: String,
    value: Long,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = T4Surface),
        border = BorderStroke(1.dp, T4Border)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = T4Primary)
            Column {
                Text(
                    text = value.toString(),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = T4Text
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = T4TextMuted
                )
            }
        }
    }
}

@Composable
private fun VerificationAdminCard(
    verification: StudentVerificationDto,
    isWorking: Boolean,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = T4Surface),
        border = BorderStroke(1.dp, T4Border)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = verification.correo,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = T4Text,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "#${verification.idUsuario} · ${verification.archivos.size} archivo(s)",
                        style = MaterialTheme.typography.bodySmall,
                        color = T4TextMuted
                    )
                }
                StatusChip(
                    text = "Pendiente",
                    containerColor = T4Mint,
                    contentColor = T4MintDark
                )
            }
            verification.archivos.forEach { attachment ->
                StoredAttachmentRow(attachment = attachment)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onReject,
                    enabled = !isWorking,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Rechazar")
                }
                Button(
                    onClick = onApprove,
                    enabled = !isWorking,
                    modifier = Modifier.weight(1f)
                ) {
                    if (isWorking) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Aprobar")
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminTaskCard(
    task: TaskDto,
    isWorking: Boolean,
    onCancel: () -> Unit
) {
    val canCancel = task.estadoTarea.equals("PUBLICADA", true) ||
        task.estadoTarea.equals("CERRADA", true)
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = T4Surface),
        border = BorderStroke(1.dp, T4Border)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = task.titulo,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = T4Text
                    )
                    Text(
                        text = "Publicada por #${task.idCliente}",
                        style = MaterialTheme.typography.bodySmall,
                        color = T4TextMuted
                    )
                }
                Text(
                    text = formatCordobas(task.presupuesto),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = T4Primary
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatusChip(text = task.estadoTarea.lowercase().replaceFirstChar(Char::uppercase))
                StatusChip(text = task.modalidad ?: "Sin modalidad")
                Spacer(modifier = Modifier.weight(1f))
                if (canCancel) {
                    TextButton(onClick = onCancel, enabled = !isWorking) {
                        Icon(
                            imageVector = Icons.Filled.Block,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.size(6.dp))
                        Text("Retirar")
                    }
                }
            }
        }
    }
}

@Composable
private fun RejectVerificationDialog(
    email: String,
    onDismiss: () -> Unit,
    onConfirm: (String?) -> Unit
) {
    var observation by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rechazar solicitud") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Indica que debe corregir $email.")
                OutlinedTextField(
                    value = observation,
                    onValueChange = { observation = it.take(300) },
                    label = { Text("Observacion") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(observation.trim().ifBlank { null }) }) {
                Text("Rechazar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

private fun formatCordobas(amount: Double): String {
    val formatter = NumberFormat.getNumberInstance(Locale.US).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }
    return "C$ ${formatter.format(amount)}"
}
