package com.videoeditor.feature.compress.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.videoeditor.core.designsys.AuroraProgressRing
import com.videoeditor.core.designsys.GlassCard
import com.videoeditor.core.designsys.GradientButton
import com.videoeditor.core.designsys.GradientButtonVariant
import com.videoeditor.core.designsys.StatPill
import com.videoeditor.core.theme.AuroraGradients
import com.videoeditor.core.theme.AuroraTextPrimary
import com.videoeditor.core.theme.AuroraTextSecondary
import com.videoeditor.feature.compress.model.EncodeProgress
import java.text.DecimalFormat

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RunningStep(
    progress: EncodeProgress,
    onCancel: () -> Unit,
) {
    val df = remember { DecimalFormat("#.#") }
    val percent = (progress.percent * 100).toInt()
    val etaText = progress.etaSeconds?.let { eta ->
        val mins = eta / 60
        val secs = eta % 60
        "ETA %d:%02d".format(mins, secs)
    } ?: "Calculating ETA…"
    val gradient = remember { AuroraGradients.horizontal() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Compressing",
            style = MaterialTheme.typography.headlineSmall,
            color = AuroraTextPrimary,
        )
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            AuroraProgressRing(fraction = progress.percent.toFloat(), diameter = 240.dp)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "$percent%",
                    style = MaterialTheme.typography.displayLarge.copy(brush = gradient),
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = etaText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = AuroraTextSecondary,
                )
            }
        }
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                StatPill(label = "Frame", value = progress.frame.toString())
                StatPill(label = "Speed", value = "${df.format(progress.fps)} fps")
                progress.etaSeconds?.let {
                    StatPill(label = "Remaining", value = "${it}s")
                }
            }
        }
        Spacer(modifier = Modifier.weight(1f))
        GradientButton(
            text = "Cancel",
            onClick = onCancel,
            variant = GradientButtonVariant.Ghost,
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = {
                Icon(
                    Icons.Default.Close,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = AuroraTextPrimary,
                )
            },
        )
        Spacer(modifier = Modifier.height(24.dp))
    }
}
