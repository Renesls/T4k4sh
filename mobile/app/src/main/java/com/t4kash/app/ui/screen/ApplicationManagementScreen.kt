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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.t4kash.app.ui.components.ConnectionErrorState
import com.t4kash.app.ui.components.EmptyState
import com.t4kash.app.ui.components.StatusChip
import com.t4kash.app.ui.components.T4PatternSurface
import com.t4kash.app.ui.components.T4TopBar
import com.t4kash.app.ui.model.ApplicationDto
import com.t4kash.app.ui.theme.T4AmberContainer
import com.t4kash.app.ui.theme.T4Border
import com.t4kash.app.ui.theme.T4Danger
import com.t4kash.app.ui.theme.T4Mint
import com.t4kash.app.ui.theme.T4MintDark
import com.t4kash.app.ui.theme.T4Surface
import com.t4kash.app.ui.theme.T4Text
import com.t4kash.app.ui.theme.T4TextMuted
import com.t4kash.app.ui.viewmodel.MarketplaceViewModel

@Composable
fun ApplicationManagementScreen(
    taskId: Int,
    viewModel: MarketplaceViewModel,
    onBack: () -> Unit
) {
    val state = viewModel.uiState
    val task = state.tasks.firstOrNull { it.idTarea == taskId }
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedFilterName by rememberSaveable {
        mutableStateOf(ApplicationFilter.ALL.name)
    }
    var pendingDecision by remember { mutableStateOf<ApplicationDecision?>(null) }

    LaunchedEffect(taskId) {
        viewModel.loadApplications(taskId)
    }

    LaunchedEffect(state.applicationActionMessage) {
        state.applicationActionMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearApplicationActionMessage()
        }
    }

    pendingDecision?.let { decision ->
        DecisionDialog(
            decision = decision,
            isSubmitting = state.updatingApplicationId != null,
            onDismiss = {
                if (state.updatingApplicationId == null) {
                    pendingDecision = null
                }
            },
            onConfirm = {
                if (decision.accept) {
                    viewModel.acceptApplication(decision.application)
                } else {
                    viewModel.rejectApplication(decision.application)
                }
                pendingDecision = null
            }
        )
    }

    val selectedFilter = ApplicationFilter.valueOf(selectedFilterName)
    val filteredApplications = state.applications.filter {
        selectedFilter.status == null ||
            it.estadoPostulacion.equals(selectedFilter.status, ignoreCase = true)
    }
    val hasAcceptedApplication = state.applications.any {
        it.estadoPostulacion.equals("ACEPTADA", ignoreCase = true)
    }

    Scaffold(
        topBar = {
            T4TopBar(
                title = "Postulaciones",
                subtitle = "Historial y decisiones",
                onBack = onBack
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        when {
            state.isLoadingApplications -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Cargando postulaciones...")
                }
            }

            state.applicationsError != null && state.applications.isEmpty() -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    ConnectionErrorState(
                        message = state.applicationsError,
                        onRetry = { viewModel.loadApplications(taskId) }
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
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = task?.titulo ?: "Oportunidad #$taskId",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = applicationSummary(state.applications),
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
                            items(ApplicationFilter.entries) { filter ->
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

                    state.applicationsError?.let { error ->
                        item {
                            Text(
                                text = error,
                                modifier = Modifier.padding(horizontal = 16.dp),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }

                    if (filteredApplications.isEmpty()) {
                        item {
                            EmptyState(
                                title = if (state.applications.isEmpty()) {
                                    "Aun no hay postulaciones"
                                } else {
                                    "Sin resultados en este estado"
                                },
                                message = if (state.applications.isEmpty()) {
                                    "Las propuestas de estudiantes apareceran aqui."
                                } else {
                                    "Selecciona otro filtro para revisar el historial."
                                },
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    } else {
                        items(
                            items = filteredApplications,
                            key = { it.idPostulacion }
                        ) { application ->
                            ApplicationCard(
                                application = application,
                                hasAcceptedApplication = hasAcceptedApplication,
                                isUpdating = state.updatingApplicationId ==
                                    application.idPostulacion,
                                actionsEnabled = state.updatingApplicationId == null,
                                onAccept = {
                                    pendingDecision = ApplicationDecision(
                                        application = application,
                                        accept = true
                                    )
                                },
                                onReject = {
                                    pendingDecision = ApplicationDecision(
                                        application = application,
                                        accept = false
                                    )
                                },
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
private fun ApplicationCard(
    application: ApplicationDto,
    hasAcceptedApplication: Boolean,
    isUpdating: Boolean,
    actionsEnabled: Boolean,
    onAccept: () -> Unit,
    onReject: () -> Unit,
    modifier: Modifier = Modifier
) {
    val statusColors = applicationStatusColors(application.estadoPostulacion)
    Card(
        modifier = modifier.fillMaxWidth(),
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
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Person,
                        contentDescription = null,
                        tint = T4TextMuted
                    )
                    Column {
                        Text(
                            text = "Estudiante #${application.idEstudiante}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = T4Text
                        )
                        Text(
                            text = application.fechaPostulacion.toReadableDate(),
                            style = MaterialTheme.typography.bodySmall,
                            color = T4TextMuted
                        )
                    }
                }
                StatusChip(
                    text = application.estadoPostulacion.lowercase()
                        .replaceFirstChar { it.uppercase() },
                    containerColor = statusColors.first,
                    contentColor = statusColors.second
                )
            }

            Text(
                text = application.mensaje?.takeIf { it.isNotBlank() }
                    ?: "El estudiante no agrego un mensaje.",
                style = MaterialTheme.typography.bodyMedium,
                color = T4TextMuted
            )

            Text(
                text = application.precioPropuesto?.let {
                    "C\$ ${"%.2f".format(it)} propuestos"
                } ?: "Sin precio propuesto",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = T4Text
            )

            if (application.estadoPostulacion.equals("PENDIENTE", ignoreCase = true)) {
                if (isUpdating) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                        Text(
                            text = "Actualizando postulacion...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = T4TextMuted
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onReject,
                            modifier = Modifier.weight(1f),
                            enabled = actionsEnabled
                        ) {
                            Text("Rechazar")
                        }
                        Button(
                            onClick = onAccept,
                            modifier = Modifier.weight(1f),
                            enabled = actionsEnabled && !hasAcceptedApplication
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.size(6.dp))
                            Text("Aceptar")
                        }
                    }
                }
                if (hasAcceptedApplication) {
                    Text(
                        text = "Ya existe una propuesta aceptada. Las restantes pueden rechazarse.",
                        style = MaterialTheme.typography.bodySmall,
                        color = T4TextMuted
                    )
                }
            }
        }
    }
}

@Composable
private fun DecisionDialog(
    decision: ApplicationDecision,
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val action = if (decision.accept) "aceptar" else "rechazar"
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = if (decision.accept) {
                    Icons.Filled.CheckCircle
                } else {
                    Icons.Filled.History
                },
                contentDescription = null
            )
        },
        title = {
            Text(
                text = if (decision.accept) {
                    "Aceptar postulacion"
                } else {
                    "Rechazar postulacion"
                }
            )
        },
        text = {
            Text(
                "Vas a $action la propuesta del estudiante " +
                    "#${decision.application.idEstudiante}. Esta decision quedara en el historial."
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = !isSubmitting
            ) {
                Text(if (decision.accept) "Aceptar" else "Rechazar")
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

private fun applicationSummary(applications: List<ApplicationDto>): String {
    val pending = applications.count {
        it.estadoPostulacion.equals("PENDIENTE", ignoreCase = true)
    }
    return "${applications.size} recibidas · $pending pendientes"
}

private fun applicationStatusColors(status: String): Pair<Color, Color> {
    return when (status.uppercase()) {
        "ACEPTADA" -> T4Mint to T4MintDark
        "RECHAZADA" -> Color(0xFFFFE4E4) to T4Danger
        else -> T4AmberContainer to T4Text
    }
}

private fun String.toReadableDate(): String {
    val date = substringBefore('T')
    val parts = date.split('-')
    return if (parts.size == 3) {
        "${parts[2]}/${parts[1]}/${parts[0]}"
    } else {
        date
    }
}

private data class ApplicationDecision(
    val application: ApplicationDto,
    val accept: Boolean
)

private enum class ApplicationFilter(
    val label: String,
    val status: String?
) {
    ALL("Todas", null),
    PENDING("Pendientes", "PENDIENTE"),
    ACCEPTED("Aceptadas", "ACEPTADA"),
    REJECTED("Rechazadas", "RECHAZADA")
}
