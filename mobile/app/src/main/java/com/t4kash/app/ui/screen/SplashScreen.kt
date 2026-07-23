package com.t4kash.app.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.t4kash.app.R
import com.t4kash.app.ui.components.StatusChip
import com.t4kash.app.ui.theme.T4BrandDark
import com.t4kash.app.ui.theme.T4Mint
import com.t4kash.app.ui.theme.T4Primary
import com.t4kash.app.ui.theme.T4PrimaryDark
import com.t4kash.app.ui.theme.T4Surface
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onFinished: () -> Unit
) {
    LaunchedEffect(Unit) {
        delay(1400)
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(T4BrandDark, T4PrimaryDark, T4Primary)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(116.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(Color.White)
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.t4kash_logo),
                    contentDescription = "Logo de T4KASH",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "T4KASH",
                color = Color.White,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Black
            )
            Text(
                text = "Conectando oportunidades reales con talento universitario",
                color = Color.White.copy(alpha = 0.78f),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))
            Row {
                StatusChip(
                    text = "Talento universitario",
                    selected = true,
                    containerColor = T4Mint,
                    contentColor = T4BrandDark
                )
                Spacer(modifier = Modifier.width(8.dp))
                StatusChip(
                    text = "Oportunidades reales",
                    selected = true,
                    containerColor = T4Surface.copy(alpha = 0.16f),
                    contentColor = Color.White
                )
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 72.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            LinearProgressIndicator(
                modifier = Modifier
                    .width(120.dp)
                    .height(3.dp)
                    .clip(RoundedCornerShape(99.dp)),
                color = Color.White,
                trackColor = T4BrandDark.copy(alpha = 0.28f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Preparando tu espacio...",
                color = Color.White.copy(alpha = 0.72f),
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}
