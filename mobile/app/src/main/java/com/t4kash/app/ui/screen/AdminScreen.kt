package com.t4kash.app.ui.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
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
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
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
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.unit.dp
import com.t4kash.app.ui.components.ConnectionErrorState
import com.t4kash.app.ui.components.EmptyState
import com.t4kash.app.ui.components.StatusChip
import com.t4kash.app.ui.components.T4PatternSurface
import com.t4kash.app.ui.components.T4TopBar
import com.t4kash.app.ui.components.keepVisibleAboveKeyboard
import com.t4kash.app.ui.model.AdminSummaryDto
import com.t4kash.app.ui.model.ReportDto
import com.t4kash.app.ui.model.PaymentDisputeDto
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
import com.t4kash.app.ui.formatApiDateTime
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
    var reportReviewTarget by remember { mutableStateOf<ReportReviewTarget?>(null) }
    var disputeResolutionTarget by remember {
        mutableStateOf<DisputeResolutionTarget?>(null)
    }

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
    reportReviewTarget?.let { target ->
        ReviewReportDialog(
            report = target.report,
            resolved = target.resolved,
            onDismiss = { reportReviewTarget = null },
            onConfirm = { observation, removeTask ->
                viewModel.reviewReport(
                    reportId = target.report.idReporte,
                    status = if (target.resolved) "RESUELTO" else "DESCARTADO",
                    observation = observation,
                    removeTask = removeTask
                )
                reportReviewTarget = null
            }
        )
    }
    disputeResolutionTarget?.let { target ->
        ResolvePaymentDisputeDialog(
            dispute = target.dispute,
            decision = target.decision,
            onDismiss = { disputeResolutionTarget = null },
            onConfirm = { resolution ->
                viewModel.resolvePaymentDispute(
                    target.dispute.idDisputa,
                    target.decision,
                    resolution
                )
                disputeResolutionTarget = null
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
                            "Perfiles",
                            "Reportes",
                            "Tareas",
                            "Finanzas"
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
                } else if (selectedTab == 1) {
                    if (state.adminReports.isEmpty()) {
                        item {
                            EmptyState(
                                title = "Sin reportes",
                                message = "Los reportes del marketplace apareceran aqui."
                            )
                        }
                    } else {
                        items(
                            items = state.adminReports.sortedWith(
                                compareByDescending<ReportDto> {
                                    it.estadoReporte.equals("PENDIENTE", true)
                                }.thenByDescending { it.fechaReporte }
                            ),
                            key = { it.idReporte }
                        ) { report ->
                            ReportAdminCard(
                                report = report,
                                isWorking =
                                    state.adminActionKey == "report:${report.idReporte}",
                                onResolve = {
                                    reportReviewTarget =
                                        ReportReviewTarget(report, resolved = true)
                                },
                                onDismiss = {
                                    reportReviewTarget =
                                        ReportReviewTarget(report, resolved = false)
                                }
                            )
                        }
                    }
                } else if (selectedTab == 2) {
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
                } else if (state.adminPaymentDisputes.isEmpty()) {
                    item {
                        EmptyState(
                            title = "Sin disputas activas",
                            message = "Los pagos congelados para revision apareceran aqui."
                        )
                    }
                } else {
                    items(
                        items = state.adminPaymentDisputes,
                        key = { it.idDisputa }
                    ) { dispute ->
                        PaymentDisputeAdminCard(
                            dispute = dispute,
                            isWorking = state.adminActionKey ==
                                "dispute:${dispute.idDisputa}",
                            onRelease = {
                                disputeResolutionTarget = DisputeResolutionTarget(
                                    dispute,
                                    "LIBERAR_ESTUDIANTE"
                                )
                            },
                            onRefund = {
                                disputeResolutionTarget = DisputeResolutionTarget(
                                    dispute,
                                    "REEMBOLSAR_CLIENTE"
                                )
                            }
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
                label = "Reportes",
                value = summary.reportesPendientes,
                icon = Icons.Filled.Flag,
                modifier = Modifier.weight(1f)
            )
            AdminMetric(
                label = "Publicadas",
                value = summary.publicacionesActivas,
                icon = Icons.Filled.TaskAlt,
                modifier = Modifier.weight(1f)
            )
        }
        AdminMetric(
            label = "Trabajos asignados",
            value = summary.trabajosAsignados,
            icon = Icons.Filled.CheckCircle,
            modifier = Modifier.fillMaxWidth()
        )
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
private fun ReportAdminCard(
    report: ReportDto,
    isWorking: Boolean,
    onResolve: () -> Unit,
    onDismiss: () -> Unit
) {
    val pending = report.estadoReporte.equals("PENDIENTE", true)
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
                Icon(
                    imageVector = Icons.Filled.Flag,
                    contentDescription = null,
                    tint = T4Primary
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = report.motivo,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = T4Text
                    )
                    Text(
                        text = report.tituloTarea ?: "Publicacion no disponible",
                        style = MaterialTheme.typography.bodyMedium,
                        color = T4TextMuted,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                StatusChip(
                    text = report.estadoReporte.lowercase()
                        .replaceFirstChar(Char::uppercase),
                    selected = pending
                )
            }
            report.descripcion?.let { description ->
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = T4Text
                )
            }
            Text(
                text = "Reporta: ${report.correoReporta ?: "#${report.idUsuarioReporta}"}",
                style = MaterialTheme.typography.bodySmall,
                color = T4TextMuted
            )
            Text(
                text = "Reportado: ${report.correoReportado ?: "#${report.idUsuarioReportado}"}",
                style = MaterialTheme.typography.bodySmall,
                color = T4TextMuted
            )
            Text(
                text = formatApiDateTime(report.fechaReporte),
                style = MaterialTheme.typography.labelMedium,
                color = T4TextMuted
            )
            if (pending) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onDismiss,
                        enabled = !isWorking,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Descartar")
                    }
                    Button(
                        onClick = onResolve,
                        enabled = !isWorking,
                        modifier = Modifier.weight(1f)
                    ) {
                        if (isWorking) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Resolver")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReviewReportDialog(
    report: ReportDto,
    resolved: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String?, Boolean) -> Unit
) {
    var observation by remember { mutableStateOf("") }
    var removeTask by remember { mutableStateOf(false) }
    AlertDialog(
        modifier = Modifier.imePadding(),
        properties = DialogProperties(decorFitsSystemWindows = false),
        onDismissRequest = onDismiss,
        title = {
            Text(if (resolved) "Resolver reporte" else "Descartar reporte")
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = report.tituloTarea ?: report.motivo,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = if (resolved) {
                        "Confirma la accion tomada por moderacion."
                    } else {
                        "El reporte quedara cerrado sin retirar la publicacion."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = T4TextMuted
                )
                OutlinedTextField(
                    value = observation,
                    onValueChange = { observation = it.take(700) },
                    label = { Text("Observacion administrativa") },
                    minLines = 3,
                    maxLines = 5,
                    supportingText = { Text("${observation.length}/700") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .keepVisibleAboveKeyboard()
                )
                if (resolved && report.idTarea != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = removeTask,
                            onCheckedChange = { removeTask = it }
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Retirar publicacion",
                                fontWeight = FontWeight.SemiBold,
                                color = T4Text
                            )
                            Text(
                                text = "Cancela la tarea y sus postulaciones pendientes.",
                                style = MaterialTheme.typography.bodySmall,
                                color = T4TextMuted
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        observation.trim().ifBlank { null },
                        resolved && removeTask
                    )
                }
            ) {
                Text(if (resolved) "Resolver" else "Descartar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

private data class ReportReviewTarget(
    val report: ReportDto,
    val resolved: Boolean
)

private data class DisputeResolutionTarget(
    val dispute: PaymentDisputeDto,
    val decision: String
)

@Composable
private fun PaymentDisputeAdminCard(
    dispute: PaymentDisputeDto,
    isWorking: Boolean,
    onRelease: () -> Unit,
    onRefund: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = T4Surface),
        border = BorderStroke(1.dp, T4Border.copy(alpha = 0.75f))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Gavel, contentDescription = null, tint = T4Primary)
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(
                        "Disputa #${dispute.idDisputa}",
                        fontWeight = FontWeight.Bold,
                        color = T4Text
                    )
                }
                StatusChip(dispute.prioridad)
            }
            Text(dispute.motivo, fontWeight = FontWeight.SemiBold, color = T4Text)
            Text(dispute.descripcion, style = MaterialTheme.typography.bodyMedium)
            Text(
                "Pago #${dispute.idPago} · ${formatCordobas(dispute.montoDisputado)}",
                style = MaterialTheme.typography.bodySmall,
                color = T4TextMuted
            )
            Text(
                "Solicita: ${dispute.solucionSolicitada.lowercase().replace('_', ' ')}",
                style = MaterialTheme.typography.bodySmall,
                color = T4TextMuted
            )
            if (isWorking) {
                CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onRelease, modifier = Modifier.weight(1f)) {
                        Text("Liberar")
                    }
                    OutlinedButton(onClick = onRefund, modifier = Modifier.weight(1f)) {
                        Text("Reembolsar")
                    }
                }
            }
        }
    }
}

@Composable
private fun ResolvePaymentDisputeDialog(
    dispute: PaymentDisputeDto,
    decision: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var resolution by remember { mutableStateOf("") }
    val release = decision == "LIBERAR_ESTUDIANTE"
    AlertDialog(
        modifier = Modifier.imePadding(),
        onDismissRequest = onDismiss,
        title = {
            Text(if (release) "Liberar pago al estudiante" else "Reembolsar al cliente")
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Esta decision resolvera la disputa #${dispute.idDisputa} y quedara auditada."
                )
                OutlinedTextField(
                    value = resolution,
                    onValueChange = { resolution = it.take(1000) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Fundamento de la resolucion") },
                    minLines = 3
                )
            }
        },
        confirmButton = {
            Button(
                enabled = resolution.trim().length >= 10,
                onClick = { onConfirm(resolution.trim()) }
            ) {
                Text("Confirmar")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
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
                        text = task.cliente?.let {
                            "Publicada por ${it.nombreCompleto} · @${it.nombreUsuario}"
                        } ?: "Perfil del autor no disponible",
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
        modifier = Modifier.imePadding(),
        properties = DialogProperties(decorFitsSystemWindows = false),
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .keepVisibleAboveKeyboard()
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
