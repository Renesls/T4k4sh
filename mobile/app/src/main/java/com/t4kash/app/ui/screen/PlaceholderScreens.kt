package com.t4kash.app.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.t4kash.app.ui.components.EmptyState
import com.t4kash.app.ui.components.T4BottomBar
import com.t4kash.app.ui.components.T4TopBar
import com.t4kash.app.ui.navigation.Routes
import com.t4kash.app.ui.theme.T4Background
import com.t4kash.app.ui.theme.T4Primary
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
        onNavigate = onNavigate
    ) {
        EmptyState(
            title = "Tu red esta lista para crecer",
            message = "Aqui apareceran perfiles verificados, recomendaciones y conexiones por carrera."
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
        onNavigate = onNavigate
    ) {
        EmptyState(
            title = "Aun no hay mensajes",
            message = "Cuando apliques o aceptes una oportunidad, la conversacion aparecera en esta seccion."
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
        onNavigate = onNavigate
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = T4Primary)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Text(
                    text = "$0.00",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = androidx.compose.ui.graphics.Color.White
                )
                Text(
                    text = "Balance disponible",
                    style = MaterialTheme.typography.bodyMedium,
                    color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.76f)
                )
            }
        }
        EmptyState(
            title = "Sin transacciones",
            message = "Los pagos aprobados y reportes financieros se mostraran aqui."
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
                title = "Aplicacion",
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
                message = "La accion esta lista en la interfaz. El siguiente paso sera conectarla al endpoint de postulaciones.",
                action = {
                    androidx.compose.material3.Button(onClick = onBackHome) {
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
                .background(T4Background),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = T4Text
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = T4TextMuted
                )
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
