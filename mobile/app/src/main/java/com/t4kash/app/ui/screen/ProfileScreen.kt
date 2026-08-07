package com.t4kash.app.ui.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.WorkHistory
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.t4kash.app.ui.components.T4BottomBar
import com.t4kash.app.ui.components.T4TopBar
import com.t4kash.app.ui.model.PendingAttachment
import com.t4kash.app.ui.session.SessionUser
import com.t4kash.app.ui.navigation.Routes
import com.t4kash.app.ui.theme.T4Background
import com.t4kash.app.ui.theme.T4Border
import com.t4kash.app.ui.theme.T4Mint
import com.t4kash.app.ui.theme.T4MintDark
import com.t4kash.app.ui.theme.T4Primary
import com.t4kash.app.ui.theme.T4PrimaryContainer
import com.t4kash.app.ui.theme.T4Surface
import com.t4kash.app.ui.theme.T4Text
import com.t4kash.app.ui.theme.T4TextMuted
import com.t4kash.app.ui.viewmodel.MarketplaceViewModel

@Composable
fun ProfileScreen(
    viewModel: MarketplaceViewModel,
    user: SessionUser,
    onNavigate: (String) -> Unit,
    onOpenPublications: (String) -> Unit,
    onOpenJobs: () -> Unit,
    onOpenWallet: () -> Unit,
    onOpenAdmin: () -> Unit,
    onLogout: () -> Unit
) {
    var verificationFiles by remember {
        mutableStateOf<List<PendingAttachment>>(emptyList())
    }
    var verificationPickerError by remember { mutableStateOf<String?>(null) }
    val needsStudentVerification =
        user.universityName != null &&
            user.roles.none { it.equals("ESTUDIANTE", ignoreCase = true) }
    val isAdmin = user.roles.any { it.equals("ADMIN", ignoreCase = true) }
    val ownTasks = viewModel.uiState.tasks.filter { it.idCliente == user.id }
    val activeTasks = ownTasks.count {
        it.estadoTarea.equals("PUBLICADA", ignoreCase = true)
    }
    val assignedTasks = ownTasks.count {
        it.estadoTarea.equals("ASIGNADA", ignoreCase = true)
    }
    val relatedJobs = viewModel.uiState.jobs.count { job ->
        job.idEstudiante == user.id ||
            ownTasks.any { it.idTarea == job.idTarea }
    }
    val completedJobs = viewModel.uiState.jobs.count { job ->
        (job.idEstudiante == user.id || ownTasks.any { it.idTarea == job.idTarea }) &&
            job.estadoTrabajo.equals("FINALIZADO", ignoreCase = true)
    }

    LaunchedEffect(Unit) {
        viewModel.refreshJobs()
    }

    Scaffold(
        containerColor = T4Background,
        topBar = {
            T4TopBar(
                title = "Perfil",
                subtitle = "Cuenta y actividad"
            )
        },
        bottomBar = {
            T4BottomBar(
                currentRoute = Routes.PROFILE,
                onNavigate = onNavigate
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = T4Surface),
                    border = BorderStroke(1.dp, T4Border),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .background(T4Primary)
                    )
                    Row(
                        modifier = Modifier.padding(18.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier.size(68.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .background(T4Primary, RoundedCornerShape(16.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = user.initials,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .align(Alignment.BottomEnd)
                                    .background(T4Mint, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.VerifiedUser,
                                    contentDescription = "Cuenta verificada",
                                    tint = T4MintDark,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = user.fullName,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = T4Text
                            )
                            Text(
                                text = user.careerName ?: user.email,
                                style = MaterialTheme.typography.bodySmall,
                                color = T4TextMuted,
                                maxLines = 2
                            )
                            Text(
                                text = "#${user.id}",
                                style = MaterialTheme.typography.labelMedium,
                                color = T4Primary
                            )
                        }
                    }
                }
            }

            item {
                ProfileStats(
                    publications = ownTasks.size,
                    active = activeTasks,
                    assigned = assignedTasks,
                    onOpenAll = { onOpenPublications("ALL") },
                    onOpenActive = { onOpenPublications("PUBLICADA") },
                    onOpenAssigned = { onOpenPublications("ASIGNADA") }
                )
            }

            item {
                CompletionCard(
                    completed = completedJobs,
                    total = relatedJobs
                )
            }

            if (needsStudentVerification) {
                item {
                    AttachmentPickerSection(
                        attachments = verificationFiles,
                        onAttachmentsChange = {
                            verificationFiles = it.take(1)
                            verificationPickerError = null
                        },
                        onError = { verificationPickerError = it },
                        enabled = !viewModel.uiState.isUploadingStudentProof,
                        title = "Validar perfil estudiantil"
                    )
                }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Adjunta una foto del carnet o una constancia universitaria.",
                            style = MaterialTheme.typography.bodySmall,
                            color = T4TextMuted
                        )
                        verificationPickerError?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        viewModel.uiState.studentProofError?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        viewModel.uiState.studentProofMessage?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodySmall,
                                color = T4Text
                            )
                        }
                        Button(
                            onClick = {
                                verificationFiles.firstOrNull()?.let {
                                    viewModel.uploadStudentVerificationProof(it)
                                }
                            },
                            enabled = verificationFiles.isNotEmpty() &&
                                !viewModel.uiState.isUploadingStudentProof,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (viewModel.uiState.isUploadingStudentProof) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Filled.VerifiedUser,
                                    contentDescription = null
                                )
                            }
                            Spacer(modifier = Modifier.size(8.dp))
                            Text("Enviar para revision")
                        }
                    }
                }
            }

            item {
                Text(
                    text = "Cuenta",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = T4Text
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = T4Surface),
                    border = BorderStroke(1.dp, T4Border.copy(alpha = 0.65f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column {
                        user.universityName?.let { university ->
                            ProfileInfoRow(
                                icon = Icons.Filled.School,
                                label = "Universidad",
                                value = university
                            )
                        }
                        user.careerName?.let { career ->
                            ProfileInfoRow(
                                icon = Icons.Filled.School,
                                label = "Carrera",
                                value = career
                            )
                        }
                        ProfileInfoRow(
                            icon = Icons.Filled.Mail,
                            label = "Correo",
                            value = user.email
                        )
                    }
                }
            }

            if (isAdmin) {
                item {
                    Text(
                        text = "Administracion",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = T4Text
                    )
                }
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onOpenAdmin),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = T4Surface),
                        border = BorderStroke(1.dp, T4Border.copy(alpha = 0.65f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(18.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AdminPanelSettings,
                                contentDescription = null,
                                tint = T4Primary,
                                modifier = Modifier.size(26.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Panel administrativo",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = T4Text
                                )
                                Text(
                                    text = "Validaciones y moderacion",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = T4TextMuted
                                )
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Abrir administracion",
                                tint = T4TextMuted
                            )
                        }
                    }
                }
            }

            item {
                Text(
                    text = "Actividad",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = T4Text
                )
            }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onOpenJobs),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = T4Surface),
                    border = BorderStroke(1.dp, T4Border.copy(alpha = 0.65f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(18.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.WorkHistory,
                            contentDescription = null,
                            tint = T4Primary,
                            modifier = Modifier.size(26.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Trabajos asignados",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = T4Text
                            )
                            Text(
                                text = "$relatedJobs acuerdos como cliente o estudiante",
                                style = MaterialTheme.typography.bodyMedium,
                                color = T4TextMuted
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Abrir trabajos asignados",
                            tint = T4TextMuted
                        )
                    }
                }
            }

            item {
                Text(
                    text = "Finanzas",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = T4Text
                )
            }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onOpenWallet),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = T4Surface),
                    border = BorderStroke(1.dp, T4Border.copy(alpha = 0.65f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(18.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AccountBalanceWallet,
                            contentDescription = null,
                            tint = T4Primary,
                            modifier = Modifier.size(26.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Wallet",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = T4Text
                            )
                            Text(
                                text = "Balance, pagos y movimientos",
                                style = MaterialTheme.typography.bodyMedium,
                                color = T4TextMuted
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Abrir Wallet",
                            tint = T4TextMuted
                        )
                    }
                }
            }

            item {
                OutlinedButton(
                    onClick = onLogout,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Filled.Logout,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text("Cerrar sesión")
                }
            }
        }
    }
}

@Composable
private fun ProfileStats(
    publications: Int,
    active: Int,
    assigned: Int,
    onOpenAll: () -> Unit,
    onOpenActive: () -> Unit,
    onOpenAssigned: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ProfileStat(
            value = publications,
            label = "Publicadas",
            icon = Icons.Filled.WorkHistory,
            onClick = onOpenAll,
            modifier = Modifier.weight(1f)
        )
        ProfileStat(
            value = active,
            label = "Activas",
            icon = Icons.Filled.VerifiedUser,
            onClick = onOpenActive,
            modifier = Modifier.weight(1f)
        )
        ProfileStat(
            value = assigned,
            label = "Asignadas",
            icon = Icons.Filled.School,
            onClick = onOpenAssigned,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun ProfileStat(
    value: Int,
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = T4Surface),
        border = BorderStroke(1.dp, T4Border),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 13.dp),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = T4Primary,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = value.toString(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                color = T4Primary
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = T4TextMuted
            )
        }
    }
}

@Composable
private fun CompletionCard(
    completed: Int,
    total: Int
) {
    val progress = if (total == 0) 0f else completed.toFloat() / total.toFloat()
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = T4Surface),
        border = BorderStroke(1.dp, T4Border),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Tasa de finalizacion",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = T4Text
                )
                Text(
                    text = "${(progress * 100).toInt()}%",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = T4MintDark
                )
            }
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = T4Mint,
                trackColor = T4PrimaryContainer
            )
            Text(
                text = if (total == 0) {
                    "Todavia no tienes trabajos asignados."
                } else {
                    "$completed de $total trabajos finalizados"
                },
                style = MaterialTheme.typography.bodySmall,
                color = T4TextMuted
            )
        }
    }
}

@Composable
private fun ProfileInfoRow(
    icon: ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
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
                color = T4TextMuted
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = T4Text
            )
        }
    }
}
