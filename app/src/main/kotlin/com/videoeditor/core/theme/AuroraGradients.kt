package com.videoeditor.core.theme

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

object AuroraGradients {
    val stops: List<Color> = listOf(AuroraViolet, AuroraMagenta, AuroraCyan)

    fun horizontal(): Brush = Brush.horizontalGradient(stops)

    fun diagonal(): Brush = Brush.linearGradient(
        colors = stops,
        start = Offset.Zero,
        end = Offset.Infinite,
    )

    fun radial(center: Offset = Offset.Unspecified, radius: Float = Float.POSITIVE_INFINITY): Brush =
        Brush.radialGradient(
            colors = listOf(AuroraMagenta.copy(alpha = 0.85f), AuroraViolet.copy(alpha = 0.0f)),
            center = center,
            radius = radius,
        )

    fun mesh(width: Float, height: Float, progress: Float): List<Brush> {
        val tau = (2 * Math.PI).toFloat()
        val r = (minOf(width, height) * 0.7f)
        val cx1 = width * 0.5f + (width * 0.25f) * kotlin.math.cos(progress * tau)
        val cy1 = height * 0.35f + (height * 0.18f) * kotlin.math.sin(progress * tau)
        val cx2 = width * 0.5f + (width * 0.30f) * kotlin.math.cos(progress * tau + Math.PI.toFloat())
        val cy2 = height * 0.65f + (height * 0.20f) * kotlin.math.sin(progress * tau + Math.PI.toFloat())

        val violetBlob = Brush.radialGradient(
            colors = listOf(AuroraViolet.copy(alpha = 0.55f), AuroraViolet.copy(alpha = 0f)),
            center = Offset(cx1, cy1),
            radius = r,
        )
        val cyanBlob = Brush.radialGradient(
            colors = listOf(AuroraCyan.copy(alpha = 0.40f), AuroraCyan.copy(alpha = 0f)),
            center = Offset(cx2, cy2),
            radius = r,
        )
        val magentaWash = Brush.radialGradient(
            colors = listOf(AuroraMagenta.copy(alpha = 0.30f), AuroraMagenta.copy(alpha = 0f)),
            center = Offset(width * 0.5f, height * 0.5f),
            radius = r * 1.1f,
        )
        return listOf(violetBlob, cyanBlob, magentaWash)
    }
}
