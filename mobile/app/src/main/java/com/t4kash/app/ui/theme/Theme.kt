package com.t4kash.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = T4Primary,
    onPrimary = Color.White,
    primaryContainer = T4PrimaryContainer,
    onPrimaryContainer = T4Primary,
    secondary = T4MintDark,
    onSecondary = Color.White,
    secondaryContainer = T4Mint,
    onSecondaryContainer = T4MintDark,
    tertiary = T4Amber,
    onTertiary = Color.White,
    tertiaryContainer = T4AmberContainer,
    onTertiaryContainer = T4BrandDark,
    background = T4Background,
    onBackground = T4Text,
    surface = T4Surface,
    onSurface = T4Text,
    surfaceVariant = T4SurfaceVariant,
    onSurfaceVariant = T4TextMuted,
    outline = T4Border,
    error = T4Danger
)

@Composable
fun T4KASHTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        content = content
    )
}
