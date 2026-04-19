package com.videoeditor.core.designsys

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.videoeditor.core.theme.AuroraBorder
import com.videoeditor.core.theme.AuroraGradients

@Composable
fun AuroraLinearProgress(
    fraction: Float,
    modifier: Modifier = Modifier,
    height: Dp = 10.dp,
) {
    val animated by animateFloatAsState(targetValue = fraction.coerceIn(0f, 1f), label = "linear-progress")
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(height / 2))
            .background(AuroraBorder),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(animated)
                .height(height)
                .clip(RoundedCornerShape(height / 2))
                .background(AuroraGradients.horizontal()),
        )
    }
}

@Composable
fun AuroraProgressRing(
    fraction: Float,
    modifier: Modifier = Modifier,
    diameter: Dp = 240.dp,
    strokeWidth: Dp = 14.dp,
) {
    val animated by animateFloatAsState(targetValue = fraction.coerceIn(0f, 1f), label = "ring-progress")
    val progressBrush = remember { AuroraGradients.diagonal() }
    Canvas(modifier = modifier.size(diameter)) {
        val stroke = strokeWidth.toPx()
        val arcSize = Size(size.width - stroke, size.height - stroke)
        val topLeft = Offset(stroke / 2f, stroke / 2f)
        drawArc(
            brush = Brush.sweepGradient(listOf(AuroraBorder, AuroraBorder)),
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = stroke, cap = StrokeCap.Round),
        )
        if (animated > 0f) {
            drawArc(
                brush = progressBrush,
                startAngle = -90f,
                sweepAngle = 360f * animated,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
        }
    }
}
