package com.videoeditor.feature.compress.ui.configuring

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.videoeditor.core.designsys.GlassCard
import com.videoeditor.core.designsys.GradientButton
import com.videoeditor.core.theme.AuroraTextPrimary
import com.videoeditor.core.theme.AuroraTextSecondary
import com.videoeditor.feature.compress.model.OutputEstimate
import java.text.DecimalFormat

@Composable
fun OutputEstimateBar(
    estimate: OutputEstimate,
    sourceSizeBytes: Long,
    onCompress: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val df = remember { DecimalFormat("#.#") }
    val targetMb = (estimate.sizeBytes / (1024.0 * 1024.0)).toFloat()
    val animatedMb by animateFloatAsState(targetMb, label = "est-mb")

    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Estimated",
                    style = MaterialTheme.typography.bodySmall,
                    color = AuroraTextSecondary,
                )
                Text(
                    text = "${df.format(animatedMb)} MB",
                    style = MaterialTheme.typography.titleMedium,
                    color = AuroraTextPrimary,
                )
                Text(
                    text = "was ${df.format(sourceSizeBytes / (1024.0 * 1024.0))} MB \u00b7 \u00d7${df.format(estimate.ratio)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = AuroraTextSecondary,
                )
            }
            GradientButton(
                text = "Compress",
                onClick = onCompress,
            )
        }
    }
}
