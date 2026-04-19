package com.videoeditor.core.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val AuroraDarkScheme = darkColorScheme(
    primary = AuroraViolet,
    onPrimary = AuroraTextPrimary,
    secondary = AuroraMagenta,
    onSecondary = AuroraTextPrimary,
    tertiary = AuroraCyan,
    onTertiary = AuroraBgBase,
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
    outlineVariant = AuroraBorder,
    error = AuroraError,
    onError = AuroraTextPrimary,
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
