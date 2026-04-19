package com.videoeditor.core.designsys

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.videoeditor.core.theme.AuroraBorder
import com.videoeditor.core.theme.AuroraGradients
import com.videoeditor.core.theme.AuroraSurface1
import com.videoeditor.core.theme.AuroraTextMuted
import com.videoeditor.core.theme.AuroraTextPrimary
import com.videoeditor.core.theme.AuroraTextSecondary

@Composable
fun AuroraChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val shape = RoundedCornerShape(14.dp)
    val bg by animateColorAsState(
        if (selected) Color.Transparent else AuroraSurface1,
        label = "chip-bg",
    )
    val textColor = when {
        !enabled -> AuroraTextMuted
        selected -> AuroraTextPrimary
        else -> AuroraTextSecondary
    }
    val borderBrush: Brush = if (selected) {
        AuroraGradients.horizontal()
    } else {
        Brush.linearGradient(listOf(AuroraBorder, AuroraBorder))
    }
    Box(
        modifier = modifier
            .clip(shape)
            .background(bg, shape)
            .border(BorderStroke(1.dp, borderBrush), shape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = textColor,
        )
    }
}
