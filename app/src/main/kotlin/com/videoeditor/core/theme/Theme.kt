package com.videoeditor.core.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val AuroraDarkScheme = darkColorScheme(
    primary = AuroraViolet,
    onPrimary = AuroraTextPrimary,
    primaryContainer = AuroraSurface2,
    onPrimaryContainer = AuroraTextPrimary,
    secondary = AuroraMagenta,
    onSecondary = AuroraTextPrimary,
    secondaryContainer = AuroraSurface2,
    onSecondaryContainer = AuroraTextPrimary,
    tertiary = AuroraCyan,
    onTertiary = AuroraBgBase,
    tertiaryContainer = AuroraSurface1,
    onTertiaryContainer = AuroraCyan,
    error = AuroraError,
    onError = AuroraTextPrimary,
    errorContainer = Color(0xFF4E0D0D),
    onErrorContainer = AuroraError,
    background = AuroraBgBase,
    onBackground = AuroraTextPrimary,
    surface = AuroraBgBase,
    onSurface = AuroraTextPrimary,
    surfaceVariant = AuroraSurface2,
    onSurfaceVariant = AuroraTextSecondary,
    surfaceContainerLowest = AuroraBgBase,
    surfaceContainerLow = AuroraSurface1,
    surfaceContainer = AuroraSurface1,
    surfaceContainerHigh = AuroraSurface2,
    surfaceContainerHighest = AuroraSurface2,
    outline = AuroraBorder,
    outlineVariant = AuroraSurface2,
    inverseSurface = AuroraTextPrimary,
    inverseOnSurface = AuroraBgBase,
    inversePrimary = AuroraViolet,
    scrim = Color(0xAA000000),
)

@Composable
fun VideoEditorTheme(
    @Suppress("UNUSED_PARAMETER") dark: Boolean = true,
    @Suppress("UNUSED_PARAMETER") dynamic: Boolean = false,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = AuroraDarkScheme,
        typography = AuroraTypography,
        content = content,
    )
}
