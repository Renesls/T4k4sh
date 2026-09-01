package com.t4kash.app.ui.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.t4kash.app.ui.components.T4TopBar
import com.t4kash.app.ui.formatApiDateTime
import com.t4kash.app.ui.model.IdentityVerificationStatusDto
import com.t4kash.app.ui.theme.T4Background
import com.t4kash.app.ui.theme.T4Border
import com.t4kash.app.ui.theme.T4BrandDark
import com.t4kash.app.ui.theme.T4Danger
import com.t4kash.app.ui.theme.T4Mint
import com.t4kash.app.ui.theme.T4MintDark
import com.t4kash.app.ui.theme.T4Primary
import com.t4kash.app.ui.theme.T4PrimaryContainer
import com.t4kash.app.ui.theme.T4Success
import com.t4kash.app.ui.theme.T4Surface
import com.t4kash.app.ui.theme.T4Text
import com.t4kash.app.ui.theme.T4TextMuted
import com.t4kash.app.ui.viewmodel.IdentityVerificationViewModel

@Composable
fun IdentityVerificationScreen(
    viewModel: IdentityVerificationViewModel,
    onBack: () -> Unit
) {
    val state = viewModel.uiState
    val uriHandler = LocalUriHandler.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var consentAccepted by rememberSaveable { mutableStateOf(false) }
    var externalFlowOpened by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.load(force = true)
    }
    LaunchedEffect(state.verificationUrl) {
        state.verificationUrl?.let { url ->
            externalFlowOpened = true
            uriHandler.openUri(url)
            viewModel.consumeVerificationUrl()
        }
    }
    DisposableEffect(lifecycleOwner, externalFlowOpened) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && externalFlowOpened) {
                externalFlowOpened = false
                viewModel.refresh()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val currentStatus = state.status?.estado ?: "NO_INICIADA"
    val canStart = currentStatus in setOf(
        "NO_INICIADA",
        "PENDIENTE",
        "EN_PROCESO",
        "RECHAZADA",
        "EXPIRADA",
        "VENCIDA",
        "ABANDONADA",
        "REQUIERE_ACCION",
        "CANCELADA"
    )
    val requiresConsent = currentStatus in setOf(
        "NO_INICIADA",
        "RECHAZADA",
        "EXPIRADA",
        "VENCIDA",
        "ABANDONADA",
        "CANCELADA"
    )

    Scaffold(
        containerColor = T4Background,
        topBar = {
            T4TopBar(
                title = "Verificar identidad",
                subtitle = "Seguridad para trabajos y pagos",
                onBack = onBack
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
            item { VerificationHero() }

            item {
                when {
                    state.isLoading && state.status == null -> LoadingCard()
                    state.status != null -> StatusCard(state.status)
                }
            }

            item { PrivacyCard() }

            if (requiresConsent) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top
                    ) {
                        Checkbox(
                            checked = consentAccepted,
                            onCheckedChange = { consentAccepted = it }
                        )
                        Text(
                            text = "Acepto abrir el flujo de Didit y que procese el documento, la prueba de vida y la coincidencia facial para verificar mi identidad.",
                            style = MaterialTheme.typography.bodySmall,
                            color = T4Text,
                            modifier = Modifier.padding(top = 12.dp, end = 4.dp)
                        )
                    }
                }
            }

            state.errorMessage?.let { error ->
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }
            }

            if (canStart) {
                item {
                    Button(
                        onClick = {
                            viewModel.clearError()
                            viewModel.start()
                        },
                        enabled = !state.isStarting && (!requiresConsent || consentAccepted),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        if (state.isStarting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.size(8.dp))
                            Text(actionLabel(currentStatus))
                        }
                    }
                }
            }

            if (state.status?.idVerificacion != null) {
                item {
                    OutlinedButton(
                        onClick = { viewModel.refresh() },
                        enabled = !state.isRefreshing,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        if (state.isRefreshing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(19.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Filled.Refresh,
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.size(8.dp))
                            Text("Actualizar estado")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VerificationHero() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = T4BrandDark)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(86.dp)
                    .background(T4Primary, RoundedCornerShape(bottomStart = 48.dp))
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 22.dp, bottom = 18.dp)
                    .size(34.dp)
                    .background(T4Mint, CircleShape)
            )
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Security,
                    contentDescription = null,
                    tint = T4Mint,
                    modifier = Modifier.size(32.dp)
                )
                Text(
                    text = "Una identidad, más confianza",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "La verificación protege a quien publica, a quien trabaja y a cada pago dentro de T4KASH.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.82f),
                    modifier = Modifier.fillMaxWidth(0.83f)
                )
            }
        }
    }
}

@Composable
private fun LoadingCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = T4Surface),
        border = BorderStroke(1.dp, T4Border)
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
            Text("Consultando estado seguro...", color = T4Text)
        }
    }
}

@Composable
private fun StatusCard(status: IdentityVerificationStatusDto) {
    val presentation = statusPresentation(status.estado)
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = T4Surface),
        border = BorderStroke(1.dp, presentation.color.copy(alpha = 0.35f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = presentation.container,
                    shape = CircleShape
                ) {
                    Icon(
                        imageVector = presentation.icon,
                        contentDescription = null,
                        tint = presentation.color,
                        modifier = Modifier.padding(10.dp).size(26.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Estado de identidad",
                        style = MaterialTheme.typography.labelMedium,
                        color = T4TextMuted
                    )
                    Text(
                        text = presentation.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = T4Text
                    )
                }
            }
            Text(
                text = status.mensaje,
                style = MaterialTheme.typography.bodyMedium,
                color = T4TextMuted
            )
            status.fechaActualizacion?.let {
                HorizontalDivider(color = T4Border)
                Text(
                    text = "Última actualización: ${formatApiDateTime(it)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = T4TextMuted
                )
            }
        }
    }
}

@Composable
private fun PrivacyCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = T4Surface),
        border = BorderStroke(1.dp, T4Border)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Antes de continuar",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = T4Text
            )
            PrivacyRow(
                title = "Didit realiza la comprobación",
                detail = "Su flujo alojado procesa el documento, la prueba de vida y la coincidencia facial."
            )
            PrivacyRow(
                title = "T4KASH guarda lo mínimo",
                detail = "Conservamos el estado, las fechas y una huella no reversible; no guardamos fotos, videos ni el número del documento."
            )
            PrivacyRow(
                title = "Tu cuenta sigue disponible",
                detail = "Si no te verificas, puedes explorar, publicar en Network y postularte. Los trabajos pagados y Wallet requieren aprobación."
            )
        }
    }
}

@Composable
private fun PrivacyRow(title: String, detail: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = Icons.Filled.CheckCircle,
            contentDescription = null,
            tint = T4Success,
            modifier = Modifier.size(20.dp)
        )
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = T4Text
            )
            Text(
                text = detail,
                style = MaterialTheme.typography.bodySmall,
                color = T4TextMuted
            )
        }
    }
}

private fun actionLabel(status: String): String {
    return when (status) {
        "PENDIENTE", "EN_PROCESO", "REQUIERE_ACCION" -> "Continuar en Didit"
        "RECHAZADA", "EXPIRADA", "VENCIDA", "ABANDONADA", "CANCELADA" ->
            "Intentar nuevamente"
        else -> "Comenzar verificación"
    }
}

private fun statusPresentation(status: String): VerificationPresentation {
    return when (status) {
        "APROBADA" -> VerificationPresentation(
            "Aprobada",
            Icons.Filled.CheckCircle,
            T4Success,
            T4Mint.copy(alpha = 0.25f)
        )
        "EN_REVISION" -> VerificationPresentation(
            "En revisión",
            Icons.Filled.HourglassTop,
            T4Primary,
            T4PrimaryContainer
        )
        "RECHAZADA" -> VerificationPresentation(
            "Rechazada",
            Icons.Filled.ErrorOutline,
            T4Danger,
            Color(0xFFFFEDEA)
        )
        "EXPIRADA", "VENCIDA", "ABANDONADA", "CANCELADA" -> VerificationPresentation(
            status.lowercase().replaceFirstChar { it.uppercase() },
            Icons.Filled.ErrorOutline,
            T4Danger,
            Color(0xFFFFEDEA)
        )
        "PENDIENTE", "EN_PROCESO", "REQUIERE_ACCION" -> VerificationPresentation(
            when (status) {
                "EN_PROCESO" -> "En proceso"
                "REQUIERE_ACCION" -> "Requiere acción"
                else -> "Pendiente"
            },
            Icons.Filled.HourglassTop,
            T4MintDark,
            T4Mint.copy(alpha = 0.28f)
        )
        else -> VerificationPresentation(
            "No iniciada",
            Icons.Filled.VerifiedUser,
            T4Primary,
            T4PrimaryContainer
        )
    }
}

private data class VerificationPresentation(
    val title: String,
    val icon: ImageVector,
    val color: Color,
    val container: Color
)
