package com.videoeditor.feature.compress.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.videoeditor.core.designsys.SectionCard
import com.videoeditor.core.probe.ProbeResult
import com.videoeditor.feature.compress.model.CompressUiState
import com.videoeditor.feature.compress.model.CompressionSettings
import com.videoeditor.feature.compress.model.RateControl
import com.videoeditor.feature.compress.model.ResolutionPreset
import com.videoeditor.feature.compress.model.SectionId
import java.text.DecimalFormat

@Composable
fun ConfiguringStep(
    state: CompressUiState.Configuring,
    onPickDifferent: () -> Unit,
    onSmartPreset: (com.videoeditor.feature.compress.model.SmartPreset) -> Unit,
    onSettingsChanged: (CompressionSettings) -> Unit,
    onSectionToggle: (SectionId) -> Unit,
    onStartEncode: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D0F))
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        VideoPreviewCard(
            source = state.source,
            onPickDifferent = onPickDifferent,
        )
        Spacer(modifier = Modifier.height(20.dp))
        ResolutionPresetChips(
            selected = state.settings.resolution,
            sourceHeight = state.source.heightPx,
            onSelect = { res ->
                onSettingsChanged(state.settings.copy(resolution = res))
            },
        )
        Spacer(modifier = Modifier.height(20.dp))
        VideoSection(
            settings = state.settings,
            expanded = state.expandedSections.contains(SectionId.VIDEO),
            onToggle = { onSectionToggle(SectionId.VIDEO) },
            onChange = onSettingsChanged,
        )
        Spacer(modifier = Modifier.height(12.dp))
        AudioSection(
            settings = state.settings,
            expanded = state.expandedSections.contains(SectionId.AUDIO),
            onToggle = { onSectionToggle(SectionId.AUDIO) },
            onChange = onSettingsChanged,
        )
        Spacer(modifier = Modifier.height(12.dp))
        AdvancedSection(
            settings = state.settings,
            expanded = state.expandedSections.contains(SectionId.ADVANCED),
            onToggle = { onSectionToggle(SectionId.ADVANCED) },
            onChange = onSettingsChanged,
        )
        Spacer(modifier = Modifier.height(20.dp))
        OutputPreviewBar(
            source = state.source,
            estimate = state.estimate,
        )
        Spacer(modifier = Modifier.height(20.dp))
        Button(
            onClick = onStartEncode,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF5B6CFF),
            ),
            shape = RoundedCornerShape(16.dp),
        ) {
            Text("Compress now", style = MaterialTheme.typography.titleMedium)
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun VideoPreviewCard(
    source: ProbeResult,
    onPickDifferent: () -> Unit,
) {
    val df = DecimalFormat("#.##")
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF1A1A1F))
            .border(1.dp, Color(0xFF3A3A42), RoundedCornerShape(20.dp))
            .clickable(onClick = onPickDifferent)
            .padding(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF242429)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = Color(0xFF5B6CFF),
                    modifier = Modifier.size(32.dp),
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = source.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${source.widthPx}×${source.heightPx}" +
                        (if (source.frameRate.isFinite() && source.frameRate > 0) " · ${df.format(source.frameRate)} fps" else "") +
                        " · ${source.videoCodec.uppercase()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFB0B0B8),
                )
                Text(
                    text = "${df.format(source.videoBitrateBps / 1000.0)} Mbps · ${df.format(source.sizeBytes / (1024.0 * 1024.0))} MB",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFB0B0B8),
                )
                Text(
                    text = formatDuration(source.durationMs),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFB0B0B8),
                )
            }
        }
    }
}

@Composable
fun ResolutionPresetChips(
    selected: ResolutionPreset?,
    sourceHeight: Int,
    onSelect: (ResolutionPreset?) -> Unit,
) {
    Column {
        Text(
            text = "Resolution",
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
        )
        Spacer(modifier = Modifier.height(12.dp))
        val presets = ResolutionPreset.entries
        val halfIndex = (presets.size + 1) / 2
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                presets.take(halfIndex).forEach { preset ->
                    PresetChip(
                        preset = preset,
                        isSelected = selected == preset,
                        isDisabled = preset.shortEdgePx > sourceHeight,
                        onSelect = { onSelect(if (selected == preset) null else preset) },
                        modifier = Modifier.weight(1f),
                    )
                }
                repeat(halfIndex - presets.take(halfIndex).size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                presets.drop(halfIndex).forEach { preset ->
                    PresetChip(
                        preset = preset,
                        isSelected = selected == preset,
                        isDisabled = preset.shortEdgePx > sourceHeight,
                        onSelect = { onSelect(if (selected == preset) null else preset) },
                        modifier = Modifier.weight(1f),
                    )
                }
                val keepSelected = selected == null
                PresetChip(
                    preset = null,
                    isSelected = keepSelected,
                    isDisabled = false,
                    onSelect = { onSelect(null) },
                    modifier = Modifier.weight(1f),
                )
                repeat(maxOf(0, halfIndex - presets.drop(halfIndex).size - 1)) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun PresetChip(
    preset: ResolutionPreset?,
    isSelected: Boolean,
    isDisabled: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val label = preset?.label ?: "Keep"
    FilterChip(
        selected = isSelected,
        onClick = onSelect,
        label = {
            Text(
                text = label,
                maxLines = 1,
            )
        },
        enabled = !isDisabled,
        modifier = modifier,
        colors = FilterChipDefaults.filterChipColors(
            containerColor = Color(0xFF1A1A1F),
            labelColor = Color(0xFFB0B0B8),
            selectedContainerColor = if (preset == null) Color(0xFF14E0C2) else Color(0xFF5B6CFF),
            selectedLabelColor = if (preset == null) Color.Black else Color.White,
            disabledContainerColor = Color(0xFF1A1A1F).copy(alpha = 0.4f),
            disabledLabelColor = Color(0xFFB0B0B8).copy(alpha = 0.4f),
        ),
        border = FilterChipDefaults.filterChipBorder(
            borderColor = Color(0xFF3A3A42),
            selectedBorderColor = if (preset == null) Color(0xFF14E0C2) else Color(0xFF5B6CFF),
            enabled = !isDisabled,
            selected = isSelected,
        ),
    )
}

@Composable
fun VideoSection(
    settings: CompressionSettings,
    expanded: Boolean,
    onToggle: () -> Unit,
    onChange: (CompressionSettings) -> Unit,
) {
    var crf by remember(settings.crf) { mutableFloatStateOf(settings.crf.toFloat()) }
    var bitrate by remember(settings.targetBitrateKbps) { mutableFloatStateOf(settings.targetBitrateKbps.toFloat()) }

    SectionCard(
        title = "Video settings",
        expanded = expanded,
        onToggle = onToggle,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            DropdownRow(
                label = "Codec",
                value = settings.codec.name,
                options = com.videoeditor.feature.compress.model.VideoCodec.entries.map { it.name },
                onSelect = { onChange(settings.copy(codec = com.videoeditor.feature.compress.model.VideoCodec.valueOf(it))) },
            )
            RateControlToggle(
                selected = settings.rateControl,
                onSelect = { onChange(settings.copy(rateControl = it)) },
            )
            if (settings.rateControl == RateControl.CRF) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text("Quality (CRF: ${crf.toInt()})", color = Color.White)
                        Text(
                            text = qualityLabel(crf.toInt()),
                            color = Color(0xFFB0B0B8),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Slider(
                        value = crf,
                        onValueChange = { crf = it },
                        onValueChangeFinished = { onChange(settings.copy(crf = crf.toInt())) },
                        valueRange = 0f..51f,
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFF5B6CFF),
                            activeTrackColor = Color(0xFF5B6CFF),
                            inactiveTrackColor = Color(0xFF3A3A42),
                        ),
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text("Smaller file", style = MaterialTheme.typography.bodySmall, color = Color(0xFFB0B0B8))
                        Text("Better quality", style = MaterialTheme.typography.bodySmall, color = Color(0xFFB0B0B8))
                    }
                }
            }
            if (settings.rateControl == RateControl.CBR || settings.rateControl == RateControl.VBR) {
                Column {
                    Text("Bitrate: ${bitrate.toInt()} kbps", color = Color.White)
                    Spacer(modifier = Modifier.height(8.dp))
                    Slider(
                        value = bitrate,
                        onValueChange = { bitrate = it },
                        onValueChangeFinished = { onChange(settings.copy(targetBitrateKbps = bitrate.toInt())) },
                        valueRange = 100f..20000f,
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFF5B6CFF),
                            activeTrackColor = Color(0xFF5B6CFF),
                            inactiveTrackColor = Color(0xFF3A3A42),
                        ),
                    )
                }
            }
            DropdownRow(
                label = "FPS",
                value = when (settings.fps) {
                    com.videoeditor.feature.compress.model.FpsChoice.KEEP -> "Keep original"
                    else -> settings.fps.name.replace("FPS_", "") + " fps"
                },
                options = listOf("Keep original", "24 fps", "30 fps", "60 fps"),
                onSelect = {
                    val fps = when (it) {
                        "Keep original" -> com.videoeditor.feature.compress.model.FpsChoice.KEEP
                        "24 fps" -> com.videoeditor.feature.compress.model.FpsChoice.FPS_24
                        "30 fps" -> com.videoeditor.feature.compress.model.FpsChoice.FPS_30
                        "60 fps" -> com.videoeditor.feature.compress.model.FpsChoice.FPS_60
                        else -> com.videoeditor.feature.compress.model.FpsChoice.KEEP
                    }
                    onChange(settings.copy(fps = fps))
                },
            )
            DropdownRow(
                label = "Encoding",
                value = settings.preset.name.lowercase().replaceFirstChar { it.uppercase() },
                options = com.videoeditor.feature.compress.model.EncodingPreset.entries.map {
                    it.name.lowercase().replaceFirstChar { c -> c.uppercase() }
                },
                onSelect = {
                    val preset = com.videoeditor.feature.compress.model.EncodingPreset.valueOf(it.uppercase())
                    onChange(settings.copy(preset = preset))
                },
            )
            DropdownRow(
                label = "Profile",
                value = settings.profile.name,
                options = com.videoeditor.feature.compress.model.H264Profile.entries.map { it.name },
                onSelect = {
                    val profile = com.videoeditor.feature.compress.model.H264Profile.valueOf(it)
                    onChange(settings.copy(profile = profile))
                },
            )
            DropdownRow(
                label = "GOP",
                value = "${settings.gopSeconds}s",
                options = listOf("1s", "2s", "5s", "10s"),
                onSelect = {
                    val gop = it.replace("s", "").toInt()
                    onChange(settings.copy(gopSeconds = gop))
                },
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
        title = "Audio settings",
        expanded = expanded,
        onToggle = onToggle,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Codec", color = Color.White, modifier = Modifier.width(80.dp))
                Text("AAC", color = Color(0xFFB0B0B8))
            }
            DropdownRow(
                label = "Bitrate",
                value = "${settings.audio.bitrateKbps} kbps",
                options = listOf("64 kbps", "96 kbps", "128 kbps", "192 kbps", "256 kbps"),
                onSelect = {
                    val bitrate = it.replace(" kbps", "").toInt()
                    onChange(settings.copy(audio = settings.audio.copy(bitrateKbps = bitrate)))
                },
            )
            ChannelsToggle(
                channels = settings.audio.channels,
                onSelect = { onChange(settings.copy(audio = settings.audio.copy(channels = it))) },
            )
        }
    }
}

@Composable
fun AdvancedSection(
    settings: CompressionSettings,
    expanded: Boolean,
    onToggle: () -> Unit,
    onChange: (CompressionSettings) -> Unit,
) {
    SectionCard(
        title = "Advanced",
        expanded = expanded,
        onToggle = onToggle,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text("Hardware acceleration", color = Color.White)
                Text(
                    text = if (settings.useHardwareAccel) "Uses HW encoder when available" else "Software encoding only",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFB0B0B8),
                )
            }
            Switch(
                checked = settings.useHardwareAccel,
                onCheckedChange = { onChange(settings.copy(useHardwareAccel = it)) },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color(0xFF14E0C2),
                    checkedTrackColor = Color(0xFF14E0C2).copy(alpha = 0.5f),
                    uncheckedThumbColor = Color(0xFFB0B0B8),
                    uncheckedTrackColor = Color(0xFF3A3A42),
                ),
            )
        }
    }
}

@Composable
fun RateControlToggle(
    selected: RateControl,
    onSelect: (RateControl) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        RateControl.entries.forEach { mode ->
            val isSelected = selected == mode
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isSelected) Color(0xFF5B6CFF) else Color(0xFF1A1A1F))
                    .border(
                        width = 1.dp,
                        color = if (isSelected) Color(0xFF5B6CFF) else Color(0xFF3A3A42),
                        shape = RoundedCornerShape(12.dp),
                    )
                    .clickable { onSelect(mode) }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = mode.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isSelected) Color.White else Color(0xFFB0B0B8),
                )
            }
        }
    }
}

@Composable
fun ChannelsToggle(
    channels: com.videoeditor.feature.compress.model.AudioChannels,
    onSelect: (com.videoeditor.feature.compress.model.AudioChannels) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        com.videoeditor.feature.compress.model.AudioChannels.entries.forEach { ch ->
            val isSelected = channels == ch
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isSelected) Color(0xFF5B6CFF) else Color(0xFF1A1A1F))
                    .border(
                        width = 1.dp,
                        color = if (isSelected) Color(0xFF5B6CFF) else Color(0xFF3A3A42),
                        shape = RoundedCornerShape(12.dp),
                    )
                    .clickable { onSelect(ch) }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = ch.name.lowercase().replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isSelected) Color.White else Color(0xFFB0B0B8),
                )
            }
        }
    }
}

@Composable
fun DropdownRow(
    label: String,
    value: String,
    options: List<String>,
    onSelect: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF242429))
                .clickable { expanded = true }
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label, color = Color(0xFFB0B0B8))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(value, color = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("▼", color = Color(0xFFB0B0B8))
            }
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(Color(0xFF1A1A1F)),
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = option,
                            color = if (option == value) Color(0xFF5B6CFF) else Color.White,
                        )
                    },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
fun OutputPreviewBar(
    source: ProbeResult,
    estimate: com.videoeditor.feature.compress.model.OutputEstimate,
) {
    val df = DecimalFormat("#.#")
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF1A1A1F))
            .border(1.dp, Color(0xFF5B6CFF).copy(alpha = 0.3f), RoundedCornerShape(16.dp))
            .padding(16.dp),
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Estimated output",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "~${df.format(estimate.sizeBytes / (1024.0 * 1024.0))} MB",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color(0xFF14E0C2),
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "was ${df.format(source.sizeBytes / (1024.0 * 1024.0))} MB · saved ×${df.format(estimate.ratio)}",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFB0B0B8),
            )
        }
    }
}

private fun formatDuration(durationMs: Long): String {
    val totalSeconds = durationMs / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}

private fun qualityLabel(crf: Int): String = when {
    crf <= 18 -> "Lossless"
    crf <= 23 -> "High quality"
    crf <= 28 -> "Medium"
    crf <= 35 -> "Low size"
    else -> "Smallest"
}