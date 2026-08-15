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
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.WorkHistory
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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
import com.t4kash.app.ui.model.JobDto
import com.t4kash.app.ui.model.TaskDto
import com.t4kash.app.ui.parseApiDateTime
import com.t4kash.app.ui.theme.T4AmberContainer
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
import java.util.Date

@Composable
fun AssignedJobsScreen(
    viewModel: MarketplaceViewModel,
    onBack: () -> Unit,
    onJobSelected: (Int) -> Unit
) {
    val state = viewModel.uiState
    val tasksById = state.tasks.associateBy { it.idTarea }
    var selectedRoleName by rememberSaveable {
        mutableStateOf(JobRoleFilter.ALL.name)
    }
    val selectedRole = JobRoleFilter.valueOf(selectedRoleName)
    val relatedJobs = state.jobs.filter { job ->
        job.belongsToDemoUser(tasksById[job.idTarea])
    }
    val visibleJobs = relatedJobs.filter { job ->
        selectedRole.matches(job, tasksById[job.idTarea])
    }

    LaunchedEffect(Unit) {
        viewModel.refreshJobs()
    }

    Scaffold(
        containerColor = T4Background,
        topBar = {
            T4TopBar(
                title = "Trabajos asignados",
                subtitle = "Seguimiento de tus acuerdos",
                onBack = onBack,
                actions = {
                    IconButton(
                        onClick = viewModel::refreshJobs,
                        enabled = !state.isLoadingJobs
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = "Actualizar trabajos"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        when {
            state.isLoadingJobs && state.jobs.isEmpty() -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.size(12.dp))
                    Text("Actualizando trabajos...")
                }
            }

            state.jobsError != null && state.jobs.isEmpty() -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    ConnectionErrorState(
                        message = state.jobsError,
                        onRetry = { viewModel.refreshJobs(force = true) }
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
                                    text = "${relatedJobs.size} trabajos vinculados",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = jobSummary(relatedJobs),
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
                            items(JobRoleFilter.entries) { filter ->
                                StatusChip(
                                    text = filter.label,
                                    selected = selectedRole == filter,
                                    modifier = Modifier.clickable {
                                        selectedRoleName = filter.name
                                    }
                                )
                            }
                        }
                    }

                    state.jobsError?.let { error ->
                        item {
                            Text(
                                text = error,
                                modifier = Modifier.padding(horizontal = 16.dp),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }

                    if (visibleJobs.isEmpty()) {
                        item {
                            EmptyState(
                                title = if (relatedJobs.isEmpty()) {
                                    "Aun no tienes trabajos asignados"
                                } else {
                                    "No hay trabajos para este filtro"
                                },
                                message = if (relatedJobs.isEmpty()) {
                                    "Cuando una postulacion sea aceptada, el trabajo aparecera aqui."
                                } else {
                                    "Selecciona otro rol para consultar tu historial."
                                },
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    } else {
                        items(
                            items = visibleJobs,
                            key = { it.idTrabajo }
                        ) { job ->
                            AssignedJobCard(
                                job = job,
                                task = tasksById[job.idTarea],
                                onClick = { onJobSelected(job.idTrabajo) },
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
private fun AssignedJobCard(
    job: JobDto,
    task: TaskDto?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val progress = job.progress()
    val statusColors = jobStatusColors(job)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = T4Surface),
        border = BorderStroke(1.dp, T4Border.copy(alpha = 0.7f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
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
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = T4Text,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = job.roleLabel(task),
                        style = MaterialTheme.typography.bodySmall,
                        color = T4TextMuted
                    )
                }
                StatusChip(
                    text = job.displayStatus(),
                    containerColor = statusColors.first,
                    contentColor = statusColors.second
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Acuerdo #${job.idTrabajo}",
                    style = MaterialTheme.typography.labelMedium,
                    color = T4TextMuted
                )
                task?.let {
                    Text(
                        text = formatNioCurrency(it.presupuesto),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = T4MintDark
                    )
                }
            }

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth(),
                color = if (job.isOverdue()) T4Danger else T4Primary,
                trackColor = T4PrimaryContainer
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = job.progressLabel(),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (job.isOverdue()) T4Danger else T4Text
                )
                Text(
                    text = "${(progress * 100).toInt()}%",
                    style = MaterialTheme.typography.labelMedium,
                    color = T4TextMuted
                )
            }

            JobDateRow(
                icon = Icons.Filled.WorkHistory,
                label = "Inicio",
                value = formatApiDateTime(job.fechaInicio)
            )
            JobDateRow(
                icon = Icons.Filled.CalendarMonth,
                label = "Entrega esperada",
                value = formatApiDateTime(job.fechaEntregaEsperada)
            )
            JobDateRow(
                icon = Icons.Filled.School,
                label = "Estudiante",
                value = job.estudiante?.let {
                    "${it.nombreCompleto} · @${it.nombreUsuario}"
                } ?: "Perfil no disponible"
            )
        }
    }
}

@Composable
private fun JobDateRow(
    icon: ImageVector,
    label: String,
    value: String
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = T4Primary,
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

private fun JobDto.belongsToDemoUser(task: TaskDto?): Boolean {
    return idEstudiante == UserSession.requireUserId() ||
        task?.idCliente == UserSession.requireUserId()
}

private fun JobDto.roleLabel(task: TaskDto?): String {
    val isClient = task?.idCliente == UserSession.requireUserId()
    val isStudent = idEstudiante == UserSession.requireUserId()
    return when {
        isClient && isStudent -> "Participas como cliente y estudiante"
        isClient -> "Participas como cliente"
        isStudent -> "Participas como estudiante"
        else -> "Trabajo vinculado"
    }
}

private fun JobDto.displayStatus(): String {
    return when {
        estadoTrabajo.equals("FINALIZADO", ignoreCase = true) -> "FINALIZADO"
        isOverdue() -> "PLAZO VENCIDO"
        else -> estadoTrabajo.replace('_', ' ')
    }
}

private fun JobDto.progressLabel(): String {
    return when {
        estadoTrabajo.equals("FINALIZADO", ignoreCase = true) -> "Trabajo finalizado"
        isOverdue() -> "Entrega fuera de plazo"
        fechaEntregaEsperada == null -> "En proceso"
        else -> "Tiempo transcurrido"
    }
}

private fun JobDto.progress(): Float {
    if (estadoTrabajo.equals("FINALIZADO", ignoreCase = true)) {
        return 1f
    }
    val start = parseApiDateTime(fechaInicio)?.time ?: return 0f
    val end = parseApiDateTime(fechaEntregaEsperada)?.time ?: return 0f
    if (end <= start) {
        return 0f
    }
    return ((System.currentTimeMillis() - start).toFloat() / (end - start))
        .coerceIn(0f, 1f)
}

private fun JobDto.isOverdue(): Boolean {
    if (estadoTrabajo.equals("FINALIZADO", ignoreCase = true)) {
        return false
    }
    val deadline = parseApiDateTime(fechaEntregaEsperada) ?: return false
    return deadline.before(Date())
}

private fun jobStatusColors(job: JobDto): Pair<Color, Color> {
    return when {
        job.estadoTrabajo.equals("FINALIZADO", ignoreCase = true) ->
            T4Mint to T4MintDark

        job.isOverdue() -> Color(0xFFFFE4E4) to T4Danger
        else -> T4AmberContainer to T4PrimaryDark
    }
}

private fun jobSummary(jobs: List<JobDto>): String {
    val active = jobs.count { it.estadoTrabajo.equals("EN_PROCESO", ignoreCase = true) }
    val completed = jobs.count { it.estadoTrabajo.equals("FINALIZADO", ignoreCase = true) }
    return "$active en proceso · $completed finalizados"
}

private enum class JobRoleFilter(val label: String) {
    ALL("Todos"),
    CLIENT("Como cliente"),
    STUDENT("Como estudiante");

    fun matches(job: JobDto, task: TaskDto?): Boolean {
        return when (this) {
            ALL -> true
            CLIENT -> task?.idCliente == UserSession.requireUserId()
            STUDENT -> job.idEstudiante == UserSession.requireUserId()
        }
    }
}
