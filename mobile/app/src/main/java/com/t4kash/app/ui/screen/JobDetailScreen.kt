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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.t4kash.app.ui.components.ConnectionErrorState
import com.t4kash.app.ui.components.EmptyState
import com.t4kash.app.ui.components.StatusChip
import com.t4kash.app.ui.components.T4PatternSurface
import com.t4kash.app.ui.components.T4TopBar
import com.t4kash.app.ui.model.DeliveryDto
import com.t4kash.app.ui.model.JobDto
import com.t4kash.app.ui.model.TaskDto
import com.t4kash.app.ui.theme.T4Background
import com.t4kash.app.ui.theme.T4Border
import com.t4kash.app.ui.theme.T4Danger
import com.t4kash.app.ui.theme.T4Mint
import com.t4kash.app.ui.theme.T4MintDark
import com.t4kash.app.ui.theme.T4Primary
import com.t4kash.app.ui.theme.T4PrimaryContainer
import com.t4kash.app.ui.theme.T4PrimaryDark
import com.t4kash.app.ui.theme.T4Surface
import com.t4kash.app.ui.theme.T4Text
import com.t4kash.app.ui.theme.T4TextMuted
import com.t4kash.app.ui.viewmodel.MarketplaceViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun JobDetailScreen(
    jobId: Int,
    viewModel: MarketplaceViewModel,
    onBack: () -> Unit
) {
    val state = viewModel.uiState
    val job = state.jobs.firstOrNull { it.idTrabajo == jobId }
    val task = state.tasks.firstOrNull { it.idTarea == job?.idTarea }
    val focusManager = LocalFocusManager.current
    var description by remember(jobId) { mutableStateOf("") }
    var validationError by remember(jobId) { mutableStateOf<String?>(null) }
    var pendingApproval by remember { mutableStateOf<DeliveryDto?>(null) }

    LaunchedEffect(jobId) {
        viewModel.clearDeliveryFeedback()
        if (job == null) {
            viewModel.refreshJobs()
        }
        viewModel.loadDeliveries(jobId)
    }

    LaunchedEffect(state.deliveryActionMessage) {
        if (state.deliveryActionMessage == "Entrega enviada correctamente.") {
            description = ""
            validationError = null
            focusManager.clearFocus()
        }
    }

    Scaffold(
        containerColor = T4Background,
        topBar = {
            T4TopBar(
                title = "Detalle del trabajo",
                subtitle = job?.let { "Acuerdo #${it.idTrabajo}" },
                onBack = onBack,
                actions = {
                    IconButton(
                        onClick = { viewModel.loadDeliveries(jobId) },
                        enabled = !state.isLoadingDeliveries
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = "Actualizar entregas"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        when {
            job == null && state.isLoadingJobs -> {
                LoadingJobState(innerPadding)
            }

            job == null -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    ConnectionErrorState(
                        message = state.jobsError ?: "No se encontro el trabajo indicado.",
                        onRetry = viewModel::refreshJobs
                    )
                }
            }

            else -> {
                val isStudent = job.idEstudiante == DEMO_DELIVERY_USER_ID
                val isClient = task?.idCliente == DEMO_DELIVERY_USER_ID
                val canSend = isStudent &&
                    job.estadoTrabajo.equals("EN_PROCESO", ignoreCase = true)

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .imePadding(),
                    contentPadding = PaddingValues(bottom = 28.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        JobSummary(
                            job = job,
                            task = task,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }

                    if (canSend) {
                        item {
                            DeliveryForm(
                                description = description,
                                validationError = validationError,
                                isSending = state.isSendingDelivery,
                                focusManager = focusManager,
                                onDescriptionChange = {
                                    if (it.length <= MAX_DELIVERY_LENGTH) {
                                        description = it
                                        validationError = null
                                    }
                                },
                                onSubmit = {
                                    when {
                                        description.isBlank() -> {
                                            validationError = "Describe el contenido de la entrega."
                                        }

                                        description.trim().length < MIN_DELIVERY_LENGTH -> {
                                            validationError =
                                                "Escribe al menos $MIN_DELIVERY_LENGTH caracteres."
                                        }

                                        else -> viewModel.submitDelivery(
                                            job.idTrabajo,
                                            description
                                        )
                                    }
                                },
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    }

                    state.deliveryActionMessage?.let { message ->
                        item {
                            Text(
                                text = message,
                                modifier = Modifier.padding(horizontal = 16.dp),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = T4MintDark
                            )
                        }
                    }

                    state.deliveriesError?.let { error ->
                        item {
                            Text(
                                text = error,
                                modifier = Modifier.padding(horizontal = 16.dp),
                                style = MaterialTheme.typography.bodyMedium,
                                color = T4Danger
                            )
                        }
                    }

                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Entregas",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = T4Text
                                )
                                Text(
                                    text = "${state.deliveries.size} registradas",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = T4TextMuted
                                )
                            }
                            if (state.isLoadingDeliveries) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            }
                        }
                    }

                    if (!state.isLoadingDeliveries && state.deliveries.isEmpty()) {
                        item {
                            EmptyState(
                                title = "Aun no hay entregas",
                                message = if (isStudent) {
                                    "Registra el primer avance o resultado del trabajo."
                                } else {
                                    "Las entregas del estudiante apareceran aqui."
                                },
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    } else {
                        items(
                            items = state.deliveries,
                            key = { it.idEntrega }
                        ) { delivery ->
                            DeliveryCard(
                                delivery = delivery,
                                canApprove = isClient &&
                                    delivery.estadoEntrega.equals(
                                        "ENVIADA",
                                        ignoreCase = true
                                    ) &&
                                    !job.estadoTrabajo.equals(
                                        "FINALIZADO",
                                        ignoreCase = true
                                    ),
                                isApproving =
                                    state.approvingDeliveryId == delivery.idEntrega,
                                onApprove = { pendingApproval = delivery },
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    pendingApproval?.let { delivery ->
        AlertDialog(
            onDismissRequest = { pendingApproval = null },
            icon = {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = T4MintDark
                )
            },
            title = { Text("Aprobar entrega") },
            text = {
                Text(
                    "Al aprobar esta entrega, el trabajo quedara marcado como finalizado."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        pendingApproval = null
                        viewModel.approveDelivery(delivery)
                    }
                ) {
                    Text("Aprobar")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingApproval = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
private fun LoadingJobState(innerPadding: PaddingValues) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator()
        Spacer(modifier = Modifier.size(12.dp))
        Text("Cargando trabajo...")
    }
}

@Composable
private fun JobSummary(
    job: JobDto,
    task: TaskDto?,
    modifier: Modifier = Modifier
) {
    T4PatternSurface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = task?.titulo ?: "Trabajo #${job.idTrabajo}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = androidx.compose.ui.graphics.Color.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Estudiante #${job.idEstudiante}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.82f)
                    )
                }
                StatusChip(
                    text = job.estadoTrabajo.replace('_', ' '),
                    containerColor = if (
                        job.estadoTrabajo.equals("FINALIZADO", ignoreCase = true)
                    ) {
                        T4Mint
                    } else {
                        T4PrimaryContainer
                    },
                    contentColor = if (
                        job.estadoTrabajo.equals("FINALIZADO", ignoreCase = true)
                    ) {
                        T4MintDark
                    } else {
                        T4PrimaryDark
                    }
                )
            }

            SummaryRow(
                icon = Icons.Filled.Payments,
                label = "Presupuesto",
                value = task?.presupuesto?.let(::formatJobCordobas) ?: "Sin monto"
            )
            SummaryRow(
                icon = Icons.Filled.Schedule,
                label = "Inicio",
                value = job.fechaInicio.toDeliveryDate()
            )
            SummaryRow(
                icon = Icons.Filled.CalendarMonth,
                label = "Entrega esperada",
                value = job.fechaEntregaEsperada.toDeliveryDate()
            )
        }
    }
}

@Composable
private fun SummaryRow(
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
            tint = T4Mint,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodySmall,
            color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.76f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = androidx.compose.ui.graphics.Color.White
        )
    }
}

@Composable
private fun DeliveryForm(
    description: String,
    validationError: String?,
    isSending: Boolean,
    focusManager: FocusManager,
    onDescriptionChange: (String) -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = T4Surface),
        border = BorderStroke(1.dp, T4Border.copy(alpha = 0.7f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Registrar entrega",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = T4Text
            )
            Text(
                text = "Resume el avance, resultado o enlace que estas entregando.",
                style = MaterialTheme.typography.bodySmall,
                color = T4TextMuted
            )
            OutlinedTextField(
                value = description,
                onValueChange = onDescriptionChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Descripcion de la entrega") },
                supportingText = {
                    Text(
                        validationError ?: "${description.length}/$MAX_DELIVERY_LENGTH"
                    )
                },
                isError = validationError != null,
                minLines = 4,
                maxLines = 7,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = { focusManager.clearFocus() }
                ),
                enabled = !isSending,
                shape = RoundedCornerShape(8.dp)
            )
            Button(
                onClick = onSubmit,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSending
            ) {
                if (isSending) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.Send,
                        contentDescription = null
                    )
                }
                Spacer(modifier = Modifier.size(8.dp))
                Text(if (isSending) "Enviando..." else "Enviar entrega")
            }
        }
    }
}

@Composable
private fun DeliveryCard(
    delivery: DeliveryDto,
    canApprove: Boolean,
    isApproving: Boolean,
    onApprove: () -> Unit,
    modifier: Modifier = Modifier
) {
    val approved = delivery.estadoEntrega.equals("APROBADA", ignoreCase = true)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = T4Surface),
        border = BorderStroke(1.dp, T4Border.copy(alpha = 0.7f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Entrega #${delivery.idEntrega}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = T4Text
                    )
                    Text(
                        text = delivery.fechaEntrega.toDeliveryDate(),
                        style = MaterialTheme.typography.bodySmall,
                        color = T4TextMuted
                    )
                }
                StatusChip(
                    text = delivery.estadoEntrega,
                    containerColor = if (approved) T4Mint else T4PrimaryContainer,
                    contentColor = if (approved) T4MintDark else T4PrimaryDark
                )
            }

            Text(
                text = delivery.descripcionEntrega,
                style = MaterialTheme.typography.bodyMedium,
                color = T4Text
            )

            if (canApprove) {
                OutlinedButton(
                    onClick = onApprove,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isApproving
                ) {
                    if (isApproving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = null
                        )
                    }
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(if (isApproving) "Aprobando..." else "Aprobar entrega")
                }
            }
        }
    }
}

private fun formatJobCordobas(amount: Double): String {
    return "C$ " + String.format(Locale.US, "%,.2f", amount)
}

private fun String?.toDeliveryDate(): String {
    if (this.isNullOrBlank()) {
        return "Sin fecha definida"
    }
    val parsed = parseDeliveryDate(this)
        ?: return substringBefore('.').replace('T', ' ')
    return SimpleDateFormat("dd/MM/yyyy · HH:mm", Locale.getDefault()).format(parsed)
}

private fun parseDeliveryDate(value: String): Date? {
    val normalized = value.substringBefore('.').take(19)
    return runCatching {
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
            isLenient = false
        }.parse(normalized)
    }.getOrNull()
}

private const val DEMO_DELIVERY_USER_ID = 1
private const val MIN_DELIVERY_LENGTH = 10
private const val MAX_DELIVERY_LENGTH = 1000
