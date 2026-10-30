package com.phishdb.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColors = darkColorScheme(
    primary = DonutRed,
    onPrimary = Color.White,
    primaryContainer = DonutRedDeep,
    onPrimaryContainer = DonutCream,
    secondary = DonutCream,
    onSecondary = DonutInk,
    tertiary = DonutMist,
    background = DonutNavyDeep,
    onBackground = DonutCream,
    surface = DonutNavy,
    onSurface = DonutCream,
    surfaceVariant = DonutHaze,
    onSurfaceVariant = DonutCream.copy(alpha = 0.85f),
    outline = DonutMist,
    outlineVariant = DonutMist.copy(alpha = 0.5f),
    error = DonutRed,
    onError = Color.White,
)

private val LightColors = lightColorScheme(
    primary = DonutRed,
    onPrimary = Color.White,
    primaryContainer = DonutRed.copy(alpha = 0.15f),
    onPrimaryContainer = DonutRedDeep,
    secondary = DonutNavy,
    onSecondary = Color.White,
    tertiary = DonutMist,
    background = DonutCream,
    onBackground = DonutInk,
    surface = Color.White,
    onSurface = DonutInk,
    surfaceVariant = DonutCream.copy(alpha = 0.6f),
    onSurfaceVariant = DonutInk.copy(alpha = 0.8f),
    outline = DonutMist,
    outlineVariant = DonutMist.copy(alpha = 0.5f),
    error = DonutRedDeep,
    onError = Color.White,
)

@Composable
fun PhishDBTheme(
    useDarkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (useDarkTheme) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colors,
        typography = PhishTypography,
        content = content,
    )
}
