package com.videoeditor.feature.compress.ui.configuring

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.videoeditor.core.designsys.GlassCard
import com.videoeditor.core.designsys.SectionHeader
import com.videoeditor.feature.compress.model.CompressionSettings
import com.videoeditor.feature.compress.ui.configuring.internal.AuroraDropdownRow
import com.videoeditor.feature.compress.ui.configuring.internal.SegmentedToggleChannels

@Composable
fun AudioSection(
    settings: CompressionSettings,
    expanded: Boolean,
    onToggle: () -> Unit,
    onChange: (CompressionSettings) -> Unit,
    modifier: Modifier = Modifier,
) {
    GlassCard(modifier = modifier.fillMaxWidth(), onClick = onToggle) {
        Column {
            SectionHeader(
                title = "Audio",
                summary = "AAC \u00b7 ${settings.audio.bitrateKbps}k \u00b7 ${settings.audio.channels.name.lowercase()}",
            )
            if (expanded) {
                Spacer(modifier = Modifier.height(16.dp))
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    AuroraDropdownRow(
                        label = "Bitrate",
                        value = "${settings.audio.bitrateKbps} kbps",
                        options = listOf("64 kbps", "96 kbps", "128 kbps", "192 kbps", "256 kbps"),
                        onSelect = {
                            val bitrate = it.replace(" kbps", "").toInt()
                            onChange(settings.copy(audio = settings.audio.copy(bitrateKbps = bitrate)))
                        },
                    )
                    SegmentedToggleChannels(
                        channels = settings.audio.channels,
                        onSelect = { onChange(settings.copy(audio = settings.audio.copy(channels = it))) },
                    )
                }
            }
        }
    }
}
