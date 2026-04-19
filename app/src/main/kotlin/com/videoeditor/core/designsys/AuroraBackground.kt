package com.videoeditor.core.designsys

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.videoeditor.core.theme.AuroraBgBase
import com.videoeditor.core.theme.AuroraGradients

@Composable
fun AuroraBackground(
    modifier: Modifier = Modifier,
    static: Boolean = false,
    content: @Composable () -> Unit,
) {
    Box(modifier = modifier.fillMaxSize().background(AuroraBgBase)) {
        if (static) {
            StaticAurora()
        } else {
            AnimatedAurora()
        }
        content()
    }
}

@Composable
private fun StaticAurora() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        AuroraGradients.mesh(size.width, size.height, progress = 0.25f).forEach { brush ->
            drawRect(brush = brush)
        }
    }
}

@Composable
private fun AnimatedAurora() {
    val transition = rememberInfiniteTransition(label = "aurora-bg")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 22_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "aurora-progress",
    )
    Canvas(modifier = Modifier.fillMaxSize()) {
        AuroraGradients.mesh(size.width, size.height, progress).forEach { brush ->
            drawRect(brush = brush)
        }
    }
}
