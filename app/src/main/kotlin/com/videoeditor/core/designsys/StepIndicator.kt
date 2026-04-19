package com.videoeditor.core.designsys

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.videoeditor.core.theme.AuroraBorder
import com.videoeditor.core.theme.AuroraGradients
import com.videoeditor.core.theme.AuroraTextPrimary
import com.videoeditor.core.theme.AuroraTextSecondary

enum class StepState { Upcoming, Active, Done }

data class Step(val id: String, val label: String)

@Composable
fun StepIndicator(
    steps: List<Step>,
    activeIndex: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        steps.forEachIndexed { idx, _ ->
            val state = when {
                idx < activeIndex -> StepState.Done
                idx == activeIndex -> StepState.Active
                else -> StepState.Upcoming
            }
            StepDot(index = idx + 1, state = state)
            if (idx != steps.lastIndex) {
                Box(
                    modifier = Modifier
                        .size(width = 16.dp, height = 1.dp)
                        .background(
                            if (idx < activeIndex) Color.White.copy(alpha = 0.4f) else AuroraBorder,
                        ),
                )
            }
        }
    }
}

@Composable
private fun StepDot(index: Int, state: StepState) {
    val borderColor by animateColorAsState(
        targetValue = when (state) {
            StepState.Done, StepState.Active -> Color.Transparent
            StepState.Upcoming -> AuroraBorder
        },
        label = "step-border",
    )
    Box(
        modifier = Modifier
            .size(28.dp)
            .then(
                when (state) {
                    StepState.Active -> Modifier.background(AuroraGradients.horizontal(), CircleShape)
                    StepState.Done -> Modifier.background(AuroraGradients.diagonal(), CircleShape)
                    StepState.Upcoming -> Modifier
                },
            )
            .border(width = 1.dp, color = borderColor, shape = CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        when (state) {
            StepState.Done -> Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = AuroraTextPrimary,
                modifier = Modifier.size(16.dp),
            )
            StepState.Active -> Text(
                text = "$index",
                color = AuroraTextPrimary,
                style = MaterialTheme.typography.labelLarge,
            )
            StepState.Upcoming -> Text(
                text = "$index",
                color = AuroraTextSecondary,
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

object CompressSteps {
    val ALL: List<Step> = listOf(
        Step("pick", "Pick"),
        Step("configure", "Configure"),
        Step("run", "Run"),
        Step("done", "Done"),
    )
}
