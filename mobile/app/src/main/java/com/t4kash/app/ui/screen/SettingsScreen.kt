package com.t4kash.app.ui.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.t4kash.app.ui.components.AppPreferences
import com.t4kash.app.ui.components.ChatBackground
import com.t4kash.app.ui.components.ChatBackgroundTheme
import com.t4kash.app.ui.components.T4TopBar
import com.t4kash.app.ui.theme.T4Background
import com.t4kash.app.ui.theme.T4Border
import com.t4kash.app.ui.theme.T4MintDark
import com.t4kash.app.ui.theme.T4Primary
import com.t4kash.app.ui.theme.T4PrimaryContainer
import com.t4kash.app.ui.theme.T4Surface
import com.t4kash.app.ui.theme.T4Text
import com.t4kash.app.ui.theme.T4TextMuted

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var selectedTheme by remember {
        mutableStateOf(AppPreferences.getChatBackgroundTheme(context))
    }

    Scaffold(
        containerColor = T4Background,
        topBar = {
            T4TopBar(
                title = "Configuración",
                subtitle = "Preferencias de la aplicación",
                onBack = onBack
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.ChatBubbleOutline,
                        contentDescription = null,
                        tint = T4Primary
                    )
                    Column {
                        Text(
                            text = "Tema del chat",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = T4Text
                        )
                        Text(
                            text = "Elige el fondo que se usará en tus conversaciones.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = T4TextMuted
                        )
                    }
                }
            }

            items(
                items = ChatBackgroundTheme.entries,
                key = { it.storageValue }
            ) { theme ->
                ChatThemeOption(
                    theme = theme,
                    selected = theme == selectedTheme,
                    onClick = {
                        selectedTheme = theme
                        AppPreferences.setChatBackgroundTheme(context, theme)
                    }
                )
            }

            item {
                Text(
                    text = "Los cambios se guardan automáticamente en este dispositivo.",
                    style = MaterialTheme.typography.bodySmall,
                    color = T4TextMuted,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun ChatThemeOption(
    theme: ChatBackgroundTheme,
    selected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = T4Surface),
        border = BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = if (selected) T4MintDark else T4Border
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (selected) 2.dp else 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(92.dp)
                    .clip(RoundedCornerShape(6.dp))
            ) {
                ChatBackground(theme = theme, modifier = Modifier.fillMaxSize())
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(start = 10.dp, top = 12.dp)
                        .width(118.dp),
                    color = Color.White,
                    shape = RoundedCornerShape(4.dp),
                    shadowElevation = 1.dp
                ) {
                    Text(
                        text = "¿Ya viste la tarea?",
                        style = MaterialTheme.typography.labelSmall,
                        color = T4Text,
                        modifier = Modifier.padding(8.dp)
                    )
                }
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 10.dp, bottom = 10.dp)
                        .width(104.dp),
                    color = T4Primary,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = "Sí, ya comienzo.",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = theme.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = T4Text
                    )
                    Text(
                        text = theme.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = T4TextMuted
                    )
                }
                RadioButton(selected = selected, onClick = onClick)
            }
            if (selected) {
                Surface(
                    color = T4PrimaryContainer,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = "Tema activo",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = T4MintDark,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}
