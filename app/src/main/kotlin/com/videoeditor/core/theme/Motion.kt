package com.videoeditor.core.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring

object AuroraMotion {
    val auroraSpring = spring<Float>(
        dampingRatio = 0.7f,
        stiffness = Spring.StiffnessMediumLow,
    )

    val pressSpring = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessLow,
    )

    val auroraEaseOut = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)

    const val DURATION_SHORT_MS = 180
    const val DURATION_MEDIUM_MS = 250
    const val DURATION_LONG_MS = 400
}
