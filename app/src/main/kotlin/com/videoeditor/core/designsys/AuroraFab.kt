package com.videoeditor.core.designsys

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.videoeditor.core.theme.AuroraGradients
import com.videoeditor.core.theme.AuroraMagenta
import com.videoeditor.core.theme.AuroraTextPrimary

@Composable
fun AuroraFab(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.95f else 1f, label = "fab-scale")
    val shape = RoundedCornerShape(28.dp)
    Row(
        modifier = modifier
            .scale(scale)
            .shadow(elevation = 18.dp, shape = shape, ambientColor = AuroraMagenta, spotColor = AuroraMagenta)
            .clip(shape)
            .background(AuroraGradients.horizontal(), shape)
            .height(56.dp)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 22.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = AuroraTextPrimary)
        Text(
            text = text,
            color = AuroraTextPrimary,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}
