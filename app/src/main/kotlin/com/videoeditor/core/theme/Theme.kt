package com.videoeditor.core.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightScheme = lightColorScheme(
    primary = IndigoSeed,
    secondary = AccentTeal,
    background = SurfaceLight,
)
private val DarkScheme = darkColorScheme(
    primary = IndigoSeed,
    secondary = AccentTeal,
    background = Color(0xFF0D0D0F),
    surface = Color(0xFF0D0D0F),
    surfaceVariant = Color(0xFF1A1A1F),
    surfaceContainerLow = Color(0xFF1A1A1F),
    surfaceContainerHigh = Color(0xFF242429),
    onSurface = Color(0xFFFFFFFF),
    onSurfaceVariant = Color(0xFFB0B0B8),
    outline = Color(0xFF3A3A42),
)

@Composable
fun VideoEditorTheme(
    dark: Boolean = isSystemInDarkTheme(),
    dynamic: Boolean = true,
    content: @Composable () -> Unit,
) {
    val ctx = LocalContext.current
    val scheme = when {
        dynamic && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (dark) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
        dark -> DarkScheme
        else -> LightScheme
    }
    MaterialTheme(colorScheme = scheme, typography = AppTypography, content = content)
}