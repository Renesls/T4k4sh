package com.t4kash.app.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.t4kash.app.ui.components.EmptyState
import com.t4kash.app.ui.components.StatusChip
import com.t4kash.app.ui.components.T4BottomBar
import com.t4kash.app.ui.components.T4TopBar
import com.t4kash.app.ui.navigation.Routes
import com.t4kash.app.ui.theme.T4Background
import com.t4kash.app.ui.theme.T4Mint
import com.t4kash.app.ui.theme.T4MintDark
import com.t4kash.app.ui.theme.T4Primary
import com.t4kash.app.ui.theme.T4PrimarySoft
import com.t4kash.app.ui.theme.T4Surface
import com.t4kash.app.ui.theme.T4Text
import com.t4kash.app.ui.theme.T4TextMuted

@Composable
fun NetworkScreen(
    onNavigate: (String) -> Unit
) {
    FeatureScreen(
        route = Routes.NETWORK,
        title = "Network",
        subtitle = "Conecta con estudiantes",
        heroTitle = "Haz crecer tu red académica.",
        heroBody = "Aquí aparecerán perfiles verificados, recomendaciones por carrera y contactos útiles para proyectos o colaboraciones.",
        onNavigate = onNavigate
    ) {
        InfoCard(
            title = "Próximo bloque",
            body = "Perfiles cercanos, filtros por carrera y accesos rápidos a conexiones guardadas.",
            chips = listOf("Carrera", "Habilidades", "Verificado")
        )
        EmptyState(
            title = "Tu red está lista para crecer",
            message = "Aún no hay conexiones cargadas para esta demo."
        )
    }
}

@Composable
fun ChatScreen(
    onNavigate: (String) -> Unit
) {
    FeatureScreen(
        route = Routes.CHAT,
        title = "Chat",
        subtitle = "Conversaciones",
        heroTitle = "Mensajes claros para avanzar rápido.",
        heroBody = "Cuando apliques o aceptes una oportunidad, esta pantalla centralizará la conversación con seguimiento directo.",
        onNavigate = onNavigate
    ) {
        InfoCard(
            title = "Próximo bloque",
            body = "Conversaciones activas, respuestas rápidas y estado de lectura.",
            chips = listOf("Directo", "Historial", "Notificaciones")
        )
        EmptyState(
            title = "Aún no hay mensajes",
            message = "Cuando exista actividad en una oportunidad, aparecerá aquí."
        )
    }
}

@Composable
fun WalletScreen(
    onNavigate: (String) -> Unit
) {
    FeatureScreen(
        route = Routes.WALLET,
        title = "Wallet",
        subtitle = "Pagos y actividad",
        heroTitle = "Seguimiento financiero simple.",
        heroBody = "Más adelante esta vista mostrará pagos, movimientos confirmados y reportes de actividad.",
        onNavigate = onNavigate
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = T4Primary),
            shape = MaterialTheme.shapes.large
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "$0.00",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Balance disponible",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.76f)
                )
            }
        }
        InfoCard(
            title = "Movimiento esperado",
            body = "Pagos aprobados, entregas cerradas y reportes financieros.",
            chips = listOf("Balance", "Pagos", "Historial")
        )
        EmptyState(
            title = "Sin transacciones",
            message = "Los movimientos aparecerán cuando conectemos el flujo de pagos."
        )
    }
}

@Composable
fun ApplicationSentScreen(
    onBackHome: () -> Unit
) {
    Scaffold(
        containerColor = T4Background,
        topBar = {
            T4TopBar(
                title = "Aplicación enviada",
                subtitle = "Estado de solicitud"
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(T4Background)
                .padding(16.dp),
            verticalArrangement = Arrangement.Center
        ) {
            EmptyState(
                title = "Solicitud preparada",
                message = "La acción está lista en la interfaz. El siguiente paso será conectarla al endpoint de postulaciones.",
                action = {
                    Button(onClick = onBackHome) {
                        Text("Volver al inicio")
                    }
                }
            )
        }
    }
}

@Composable
private fun FeatureScreen(
    route: String,
    title: String,
    subtitle: String,
    heroTitle: String,
    heroBody: String,
    onNavigate: (String) -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Scaffold(
        containerColor = T4Background,
        topBar = {
            T4TopBar(
                title = title,
                subtitle = subtitle
            )
        },
        bottomBar = {
            T4BottomBar(
                currentRoute = route,
                onNavigate = onNavigate
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(T4Background, Color(0xFFF4F7FF))
                    )
                ),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = T4Surface),
                    shape = MaterialTheme.shapes.large
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        StatusChip(
                            text = title,
                            selected = true,
                            containerColor = T4Mint,
                            contentColor = T4MintDark
                        )
                        Text(
                            text = heroTitle,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = T4Text
                        )
                        Text(
                            text = heroBody,
                            style = MaterialTheme.typography.bodyMedium,
                            color = T4TextMuted
                        )
                    }
                }
            }
            item {
                Column(
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    content = content
                )
            }
        }
    }
}

@Composable
private fun InfoCard(
    title: String,
    body: String,
    chips: List<String>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = T4Surface),
        shape = MaterialTheme.shapes.large
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = T4Text
            )
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = T4TextMuted
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                chips.forEach { chip ->
                    StatusChip(text = chip)
                }
            }
        }
    }
}
