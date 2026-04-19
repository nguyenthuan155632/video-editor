package com.videoeditor.core.theme

import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Glassmorphism surface treatment.
 *
 * API 31+: applies RenderEffect blur to the background fill layer so the surface appears frosted.
 * Note: Compose does not expose true backdrop-blur (blurring what's behind the window);
 * this blurs the fill layer itself. The visual result is a subtle depth cue, not a literal
 * frosted-glass see-through. A solid translucent fill achieves the same perceived effect.
 *
 * API 26–30: plain translucent fill + gradient hairline border (no blur, zero perf cost).
 */
fun Modifier.glass(
    cornerRadius: Dp = 24.dp,
    surfaceAlpha: Float = 0.55f,
    borderAlpha: Float = 0.22f,
    blurRadius: Dp = 24.dp,
): Modifier = composed {
    val shape = RoundedCornerShape(cornerRadius)
    val borderBrush = Brush.linearGradient(
        listOf(
            AuroraViolet.copy(alpha = borderAlpha),
            AuroraMagenta.copy(alpha = borderAlpha),
            AuroraCyan.copy(alpha = borderAlpha),
        ),
    )
    // Apply blur only to the background fill, keeping child content unblurred.
    val bgModifier: Modifier = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        Modifier
            .graphicsLayer {
                renderEffect = RenderEffect
                    .createBlurEffect(blurRadius.toPx(), blurRadius.toPx(), Shader.TileMode.DECAL)
                    .asComposeRenderEffect()
            }
            .background(AuroraSurface1.copy(alpha = surfaceAlpha), shape)
    } else {
        Modifier.background(AuroraSurface1.copy(alpha = surfaceAlpha), shape)
    }

    this
        .clip(shape)
        .then(bgModifier)
        .drawWithContent {
            drawContent()
            drawRoundRect(
                brush = borderBrush,
                cornerRadius = CornerRadius(cornerRadius.toPx()),
                style = Stroke(width = 1.dp.toPx()),
            )
        }
}
