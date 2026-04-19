package com.videoeditor.core.theme

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring

val MotionSpec = object {
    val press = spring<Float>(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
}