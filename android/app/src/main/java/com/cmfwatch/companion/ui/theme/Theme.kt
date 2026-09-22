package com.cmfwatch.companion.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = AccentRed,
    secondary = Color(0xFF9AA0A8),
    tertiary = AccentActivity,
    background = Color(0xFF090A0B),
    surface = Color(0xFF111316),
    surfaceVariant = Color(0xFF171A1E),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color(0xFFF2F4F7),
    onSurface = Color(0xFFF2F4F7)
)

private val LightColorScheme = lightColorScheme(
    primary = AccentRed,
    secondary = Color(0xFF666666),
    tertiary = AccentActivity,
    background = Color(0xFFF5F5F7),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFE5E5EA),
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onBackground = Color.Black,
    onSurface = Color.Black
)

@Composable
fun CmfWatchTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
