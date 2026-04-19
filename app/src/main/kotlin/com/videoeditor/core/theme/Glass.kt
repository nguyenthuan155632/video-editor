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
    val blurredLayer = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        Modifier.graphicsLayer {
            renderEffect = RenderEffect
                .createBlurEffect(blurRadius.toPx(), blurRadius.toPx(), Shader.TileMode.DECAL)
                .asComposeRenderEffect()
        }
    } else {
        Modifier
    }
    this
        .clip(shape)
        .then(blurredLayer)
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
