package com.t4kash.app.ui.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.WorkHistory
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.t4kash.app.ui.components.ConnectionErrorState
import com.t4kash.app.ui.components.T4TopBar
import com.t4kash.app.ui.formatApiDateTime
import com.t4kash.app.ui.model.RatingDto
import com.t4kash.app.ui.theme.T4Background
import com.t4kash.app.ui.theme.T4Border
import com.t4kash.app.ui.theme.T4Mint
import com.t4kash.app.ui.theme.T4MintDark
import com.t4kash.app.ui.theme.T4Primary
import com.t4kash.app.ui.theme.T4Surface
import com.t4kash.app.ui.theme.T4Text
import com.t4kash.app.ui.theme.T4TextMuted
import com.t4kash.app.ui.viewmodel.PublicProfileViewModel

@Composable
fun PublicProfileScreen(
    username: String,
    viewModel: PublicProfileViewModel,
    onBack: () -> Unit
) {
    val state = viewModel.uiState

    LaunchedEffect(username) {
        viewModel.load(username)
    }

    Scaffold(
        containerColor = T4Background,
        topBar = {
            T4TopBar(
                title = "Perfil publico",
                subtitle = "Identidad T4KASH",
                onBack = onBack
            )
        }
    ) { innerPadding ->
        when {
            state.isLoading -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = T4Primary)
            }

            state.errorMessage != null -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp)
            ) {
                ConnectionErrorState(
                    message = state.errorMessage,
                    onRetry = { viewModel.load(username) }
                )
            }

            state.profile != null -> {
                val profile = state.profile
                val identity = profile.identidad
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = T4Surface),
                            border = BorderStroke(1.dp, T4Border)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(T4Primary)
                                    .padding(20.dp)
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(64.dp)
                                            .background(Color.White, RoundedCornerShape(16.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = initials(identity.nombreCompleto),
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Black,
                                            color = T4Primary
                                        )
                                    }
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(
                                            text = identity.nombreCompleto,
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                        Text(
                                            text = "@${identity.nombreUsuario}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = T4Mint
                                        )
                                        if (identity.estudianteVerificado) {
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(5.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.VerifiedUser,
                                                    contentDescription = null,
                                                    tint = T4Mint,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Text(
                                                    text = "Estudiante verificado",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = Color.White
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            PublicStat(
                                value = profile.publicaciones.toString(),
                                label = "Publicaciones",
                                modifier = Modifier.weight(1f)
                            )
                            PublicStat(
                                value = profile.trabajosCompletados.toString(),
                                label = "Completados",
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    item {
                        ReputationCard(
                            promedio = profile.promedioCalificacion,
                            total = profile.totalCalificaciones,
                            insignia = profile.insignia
                        )
                    }

                    if (profile.ultimasResenas.isNotEmpty()) {
                        item {
                            Text(
                                text = "Ultimas reseñas",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = T4Text
                            )
                        }
                        items(profile.ultimasResenas) { review ->
                            ReviewCard(review = review)
                        }
                    }

                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = T4Surface),
                            border = BorderStroke(1.dp, T4Border),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(18.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                PublicDetail(
                                    title = "Universidad",
                                    value = identity.nombreUniversidad ?: "Perfil independiente",
                                    icon = Icons.Filled.School
                                )
                                identity.nombreCarrera?.let {
                                    PublicDetail(
                                        title = "Carrera",
                                        value = it,
                                        icon = Icons.Filled.School
                                    )
                                }
                                PublicDetail(
                                    title = "Miembro desde",
                                    value = formatApiDateTime(profile.miembroDesde),
                                    icon = Icons.Filled.WorkHistory
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReputationCard(
    promedio: Double?,
    total: Long,
    insignia: String?,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = T4Surface),
        border = BorderStroke(1.dp, T4Border),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Reputacion",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = T4Text,
                    modifier = Modifier.weight(1f)
                )
                insignia?.let {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.WorkspacePremium,
                            contentDescription = null,
                            tint = T4MintDark,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = it,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = T4MintDark
                        )
                    }
                }
            }

            if (promedio == null || total == 0L) {
                Text(
                    text = "Todavia no tiene calificaciones.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = T4TextMuted
                )
            } else {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row {
                        repeat(5) { index ->
                            val filled = index < kotlin.math.round(promedio).toInt()
                            Icon(
                                imageVector = if (filled) {
                                    Icons.Filled.Star
                                } else {
                                    Icons.Filled.StarBorder
                                },
                                contentDescription = null,
                                tint = if (filled) T4MintDark else T4TextMuted,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Text(
                        text = "%.1f".format(promedio),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = T4Text
                    )
                    Text(
                        text = "($total ${if (total == 1L) "calificacion" else "calificaciones"})",
                        style = MaterialTheme.typography.bodySmall,
                        color = T4TextMuted
                    )
                }
            }
        }
    }
}

@Composable
private fun ReviewCard(review: RatingDto, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = T4Surface),
        border = BorderStroke(1.dp, T4Border),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = review.calificador?.let { "@${it.nombreUsuario}" }
                        ?: "Usuario T4KASH",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = T4Text
                )
                Row {
                    repeat(5) { index ->
                        Icon(
                            imageVector = if (index < review.puntuacion) {
                                Icons.Filled.Star
                            } else {
                                Icons.Filled.StarBorder
                            },
                            contentDescription = null,
                            tint = if (index < review.puntuacion) T4MintDark else T4TextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
            review.comentario?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium, color = T4Text)
            }
            Text(
                text = formatApiDateTime(review.fechaCalificacion),
                style = MaterialTheme.typography.bodySmall,
                color = T4TextMuted
            )
        }
    }
}

@Composable
private fun PublicStat(value: String, label: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = T4Surface),
        border = BorderStroke(1.dp, T4Border),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, fontWeight = FontWeight.Black, color = T4Primary)
            Text(label, style = MaterialTheme.typography.bodySmall, color = T4TextMuted)
        }
    }
}

@Composable
private fun PublicDetail(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .background(T4Mint, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = T4MintDark)
        }
        Column {
            Text(title, style = MaterialTheme.typography.bodySmall, color = T4TextMuted)
            Text(value, fontWeight = FontWeight.SemiBold, color = T4Text)
        }
    }
}

private fun initials(fullName: String): String {
    return fullName.split(" ")
        .mapNotNull { it.firstOrNull()?.uppercase() }
        .joinToString("")
        .take(2)
        .ifBlank { "TK" }
}
