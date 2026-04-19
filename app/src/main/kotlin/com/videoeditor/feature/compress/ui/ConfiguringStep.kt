package com.videoeditor.feature.compress.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.videoeditor.core.probe.ProbeResult
import com.videoeditor.feature.compress.model.CompressUiState
import com.videoeditor.feature.compress.model.CompressionSettings
import com.videoeditor.feature.compress.model.SectionId
import com.videoeditor.feature.compress.model.SmartPreset
import java.text.DecimalFormat

@Composable
fun ConfiguringStep(
    state: CompressUiState.Configuring,
    onPickDifferent: () -> Unit,
    onSmartPreset: (SmartPreset) -> Unit,
    onSettingsChanged: (CompressionSettings) -> Unit,
    onSectionToggle: (SectionId) -> Unit,
    onStartEncode: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        VideoPreviewCard(state.source)
        Spacer(modifier = Modifier.height(16.dp))
        SmartPresetChips(
            active = state.activeSmartPreset,
            onSelect = onSmartPreset,
        )
        Spacer(modifier = Modifier.height(16.dp))
        VideoSection(
            settings = state.settings,
            expanded = state.expandedSections.contains(SectionId.VIDEO),
            onToggle = { onSectionToggle(SectionId.VIDEO) },
            onChange = onSettingsChanged,
        )
        Spacer(modifier = Modifier.height(8.dp))
        AudioSection(
            settings = state.settings,
            expanded = state.expandedSections.contains(SectionId.AUDIO),
            onToggle = { onSectionToggle(SectionId.AUDIO) },
            onChange = onSettingsChanged,
        )
        Spacer(modifier = Modifier.height(16.dp))
        OutputPreviewBar(
            source = state.source,
            estimate = state.estimate,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onStartEncode,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Start Compression")
        }
    }
}

@Composable
fun VideoPreviewCard(source: ProbeResult) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            val df = DecimalFormat("#.##")
            Text(source.displayName, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "${source.widthPx}×${source.heightPx} · ${df.format(source.frameRate)} fps · ${source.videoCodec.uppercase()}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                "${df.format(source.videoBitrateBps / 1000.0)} Mbps · ${df.format(source.sizeBytes / (1024.0 * 1024.0))} MB",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun SmartPresetChips(
    active: SmartPreset?,
    onSelect: (SmartPreset) -> Unit,
) {
    Column {
        Text("Smart presets", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SmartPreset.entries.forEach { preset ->
                FilterChip(
                    selected = active == preset,
                    onClick = { onSelect(preset) },
                    label = { Text(preset.label) },
                )
            }
        }
    }
}

@Composable
fun VideoSection(
    settings: CompressionSettings,
    expanded: Boolean,
    onToggle: () -> Unit,
    onChange: (CompressionSettings) -> Unit,
) {
    var crf by remember(settings.crf) { mutableFloatStateOf(settings.crf.toFloat()) }

    SectionCard(
        title = "Video",
        expanded = expanded,
        onToggle = onToggle,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("CRF")
                Text("${crf.toInt()}", style = MaterialTheme.typography.bodyMedium)
            }
            Slider(
                value = crf,
                onValueChange = { crf = it },
                onValueChangeFinished = {
                    onChange(settings.copy(crf = crf.toInt()))
                },
                valueRange = 0f..51f,
            )
        }
    }
}

@Composable
fun AudioSection(
    settings: CompressionSettings,
    expanded: Boolean,
    onToggle: () -> Unit,
    onChange: (CompressionSettings) -> Unit,
) {
    SectionCard(
        title = "Audio",
        expanded = expanded,
        onToggle = onToggle,
    ) {
        Text("Codec: AAC · ${settings.audio.bitrateKbps} kbps", style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun OutputPreviewBar(
    source: ProbeResult,
    estimate: com.videoeditor.feature.compress.model.OutputEstimate,
) {
    val df = DecimalFormat("#.#")
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = androidx.compose.material3.CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Estimated output: ~${df.format(estimate.sizeBytes / (1024.0 * 1024.0))} MB (×${df.format(estimate.ratio)})",
                style = MaterialTheme.typography.titleMedium,
            )
            estimate.notes.forEach { note ->
                Text(
                    note,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
        }
    }
}

@Composable
fun SectionCard(
    title: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable () -> Unit,
) {
    Card(
        onClick = onToggle,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(if (expanded) "▼" else "▶", style = MaterialTheme.typography.bodyMedium)
            }
            if (expanded) {
                Spacer(modifier = Modifier.height(12.dp))
                content()
            }
        }
    }
}