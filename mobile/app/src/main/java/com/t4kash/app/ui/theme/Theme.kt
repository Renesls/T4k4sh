package com.t4kash.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = T4PrimaryContainer,
    onPrimary = T4Text,
    primaryContainer = T4Primary,
    onPrimaryContainer = Color.White,
    secondary = T4Mint,
    onSecondary = T4Text,
    tertiary = T4AmberContainer,
    background = T4Text,
    onBackground = Color.White,
    surface = Color(0xFF202633),
    onSurface = Color.White,
    surfaceVariant = Color(0xFF303747),
    onSurfaceVariant = T4PrimaryContainer,
    error = T4Danger
)

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
    onTertiaryContainer = T4Amber,
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
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
