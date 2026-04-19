package com.videoeditor.feature.compress.ui.configuring

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.videoeditor.core.designsys.GlassCard
import com.videoeditor.core.designsys.StatPill
import com.videoeditor.core.probe.ProbeResult
import com.videoeditor.core.theme.AuroraTextPrimary
import java.text.DecimalFormat

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VideoHeroCard(source: ProbeResult, modifier: Modifier = Modifier) {
    val df = DecimalFormat("#.##")
    GlassCard(modifier = modifier.fillMaxWidth()) {
        Column {
            Text(
                text = source.displayName,
                style = MaterialTheme.typography.titleMedium,
                color = AuroraTextPrimary,
            )
            Spacer(modifier = Modifier.height(12.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                StatPill(label = "Size", value = "${df.format(source.sizeBytes / (1024.0 * 1024.0))} MB")
                StatPill(label = "Res", value = "${source.widthPx}\u00d7${source.heightPx}")
                if (source.frameRate.isFinite() && source.frameRate > 0) {
                    StatPill(label = "FPS", value = df.format(source.frameRate))
                }
                StatPill(label = "Codec", value = source.videoCodec.uppercase())
                StatPill(label = "Length", value = formatDuration(source.durationMs))
                StatPill(label = "Bitrate", value = "${df.format(source.videoBitrateBps / 1_000_000.0)} Mbps")
            }
        }
    }
}

private fun formatDuration(durationMs: Long): String {
    val totalSeconds = durationMs / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
