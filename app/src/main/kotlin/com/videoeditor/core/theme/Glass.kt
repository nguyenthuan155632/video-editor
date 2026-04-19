package com.videoeditor.core.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Glassmorphism surface treatment: translucent fill + gradient hairline border.
 *
 * True backdrop blur (blurring what's behind) is not achievable in Compose without blurring
 * child content too — RenderEffect on a graphicsLayer captures the entire subtree.
 * Translucency against the animated AuroraBackground gives the same frosted-glass perception
 * at zero cost and without content degradation.
 */
fun Modifier.glass(
    cornerRadius: Dp = 24.dp,
    surfaceAlpha: Float = 0.55f,
    borderAlpha: Float = 0.22f,
    @Suppress("UNUSED_PARAMETER") blurRadius: Dp = 24.dp,
): Modifier = composed {
    val shape = RoundedCornerShape(cornerRadius)
    val borderBrush = Brush.linearGradient(
        listOf(
            AuroraViolet.copy(alpha = borderAlpha),
            AuroraMagenta.copy(alpha = borderAlpha),
            AuroraCyan.copy(alpha = borderAlpha),
        ),
    )
    this
        .clip(shape)
        .background(AuroraSurface1.copy(alpha = surfaceAlpha), shape)
        .drawWithContent {
            drawContent()
            drawRoundRect(
                brush = borderBrush,
                cornerRadius = CornerRadius(cornerRadius.toPx()),
                style = Stroke(width = 1.dp.toPx()),
            )
        }
}
