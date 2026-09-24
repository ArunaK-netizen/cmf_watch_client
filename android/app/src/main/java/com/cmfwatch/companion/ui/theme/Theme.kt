package com.cmfwatch.companion.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val CmfLightColorScheme = lightColorScheme(
    primary = CmfPrimary,
    onPrimary = CmfOnPrimary,
    primaryContainer = CmfPrimaryContainer,
    onPrimaryContainer = CmfOnPrimaryContainer,
    secondary = CmfSecondary,
    onSecondary = CmfOnSecondary,
    secondaryContainer = CmfSecondaryContainer,
    tertiary = CmfTertiary,
    onTertiary = CmfOnTertiary,
    tertiaryContainer = CmfTertiaryContainer,
    background = CmfBackground,
    onBackground = CmfOnSurface,
    surface = CmfSurface,
    onSurface = CmfOnSurface,
    surfaceVariant = CmfSurfaceHighest,
    onSurfaceVariant = CmfOnSurfaceVariant,
    outline = CmfOutline,
    outlineVariant = CmfOutlineVariant,
    error = CmfError,
    errorContainer = CmfErrorContainer,
    inverseSurface = Color(0xFF2F3133),
    inverseOnSurface = Color(0xFFF1F0F3),
    inversePrimary = Color(0xFFFFB5A0)
)

@Composable
fun CmfWatchTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = CmfLightColorScheme,
        typography = Typography,
        content = content
    )
}
