package com.t4kash.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = T4PrimarySoft,
    onPrimary = Color.White,
    primaryContainer = T4PrimaryDark,
    onPrimaryContainer = Color.White,
    secondary = T4Mint,
    onSecondary = T4BrandDark,
    secondaryContainer = T4MintDark,
    onSecondaryContainer = T4Mint,
    tertiary = T4AmberContainer,
    onTertiary = T4BrandDark,
    background = T4BrandDark,
    onBackground = Color.White,
    surface = Color(0xFF242424),
    onSurface = Color.White,
    surfaceVariant = Color(0xFF32322F),
    onSurfaceVariant = Color(0xFFE7E7E0),
    outline = Color(0xFF777772),
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
