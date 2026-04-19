package com.videoeditor.core.designsys

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.videoeditor.core.theme.AuroraGradients
import com.videoeditor.core.theme.AuroraTextMuted
import com.videoeditor.core.theme.AuroraTextPrimary
import com.videoeditor.core.theme.glass

enum class GradientButtonVariant { Filled, Tonal, Ghost }

@Composable
fun GradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: GradientButtonVariant = GradientButtonVariant.Filled,
    enabled: Boolean = true,
    height: Dp = 56.dp,
    cornerRadius: Dp = 20.dp,
    leadingIcon: (@Composable () -> Unit)? = null,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.97f else 1f, label = "btn-scale")

    val shape = RoundedCornerShape(cornerRadius)
    val baseModifier = modifier
        .height(height)
        .scale(scale)
        .clip(shape)
        .clickable(
            interactionSource = interaction,
            indication = null,
            enabled = enabled,
            onClick = onClick,
        )

    val styledModifier = when (variant) {
        GradientButtonVariant.Filled -> baseModifier.background(AuroraGradients.horizontal(), shape)
        GradientButtonVariant.Tonal -> baseModifier.glass(cornerRadius = cornerRadius, surfaceAlpha = 0.65f)
        GradientButtonVariant.Ghost -> baseModifier.glass(cornerRadius = cornerRadius, surfaceAlpha = 0.0f, borderAlpha = 0.6f)
    }

    Box(
        modifier = styledModifier.padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        CompositionLocalProvider(LocalTextStyle provides MaterialTheme.typography.labelLarge) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                leadingIcon?.invoke()
                Text(
                    text = text,
                    color = if (enabled) AuroraTextPrimary else AuroraTextMuted,
                )
            }
        }
    }
}
