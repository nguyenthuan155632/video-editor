package com.videoeditor.feature.compress.ui.configuring

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.videoeditor.core.designsys.GlassCard
import com.videoeditor.core.designsys.SectionHeader
import com.videoeditor.core.theme.AuroraBorder
import com.videoeditor.core.theme.AuroraTextPrimary
import com.videoeditor.core.theme.AuroraTextSecondary
import com.videoeditor.core.theme.AuroraViolet
import com.videoeditor.feature.compress.model.CompressionSettings
import com.videoeditor.feature.compress.model.EncodingPreset
import com.videoeditor.feature.compress.model.FpsChoice
import com.videoeditor.feature.compress.model.H264Profile
import com.videoeditor.feature.compress.model.RateControl
import com.videoeditor.feature.compress.model.VideoCodec
import com.videoeditor.feature.compress.ui.configuring.internal.AuroraDropdownRow
import com.videoeditor.feature.compress.ui.configuring.internal.SegmentedToggleRateControl

@Composable
fun VideoSection(
    settings: CompressionSettings,
    expanded: Boolean,
    onToggle: () -> Unit,
    onChange: (CompressionSettings) -> Unit,
    modifier: Modifier = Modifier,
) {
    var crf by remember(settings.crf) { mutableFloatStateOf(settings.crf.toFloat()) }
    var bitrate by remember(settings.targetBitrateKbps) { mutableFloatStateOf(settings.targetBitrateKbps.toFloat()) }

    GlassCard(modifier = modifier.fillMaxWidth(), onClick = onToggle) {
        Column {
            SectionHeader(
                title = "Video",
                summary = "${settings.codec.name} \u00b7 ${settings.rateControl.name} ${
                    if (settings.rateControl == RateControl.CRF) settings.crf.toString()
                    else "${settings.targetBitrateKbps}k"
                }",
            )
            if (expanded) {
                Spacer(modifier = Modifier.height(16.dp))
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    AuroraDropdownRow(
                        label = "Codec",
                        value = settings.codec.name,
                        options = VideoCodec.entries.map { it.name },
                        onSelect = { onChange(settings.copy(codec = VideoCodec.valueOf(it))) },
                    )
                    SegmentedToggleRateControl(
                        selected = settings.rateControl,
                        onSelect = { onChange(settings.copy(rateControl = it)) },
                    )
                    if (settings.rateControl == RateControl.CRF) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text("Quality (CRF ${crf.toInt()})", color = AuroraTextPrimary)
                                Text(
                                    qualityLabel(crf.toInt()),
                                    color = AuroraTextSecondary,
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                            Slider(
                                value = crf,
                                onValueChange = { crf = it },
                                onValueChangeFinished = { onChange(settings.copy(crf = crf.toInt())) },
                                valueRange = 0f..51f,
                                colors = SliderDefaults.colors(
                                    thumbColor = AuroraViolet,
                                    activeTrackColor = AuroraViolet,
                                    inactiveTrackColor = AuroraBorder,
                                ),
                            )
                        }
                    } else {
                        Column {
                            Text("Bitrate ${bitrate.toInt()} kbps", color = AuroraTextPrimary)
                            Slider(
                                value = bitrate,
                                onValueChange = { bitrate = it },
                                onValueChangeFinished = { onChange(settings.copy(targetBitrateKbps = bitrate.toInt())) },
                                valueRange = 100f..20000f,
                                colors = SliderDefaults.colors(
                                    thumbColor = AuroraViolet,
                                    activeTrackColor = AuroraViolet,
                                    inactiveTrackColor = AuroraBorder,
                                ),
                            )
                        }
                    }
                    AuroraDropdownRow(
                        label = "FPS",
                        value = when (settings.fps) {
                            FpsChoice.KEEP -> "Keep original"
                            else -> settings.fps.name.replace("FPS_", "") + " fps"
                        },
                        options = listOf("Keep original", "24 fps", "30 fps", "60 fps", "90 fps", "120 fps"),
                        onSelect = {
                            val fps = when (it) {
                                "Keep original" -> FpsChoice.KEEP
                                "24 fps" -> FpsChoice.FPS_24
                                "30 fps" -> FpsChoice.FPS_30
                                "60 fps" -> FpsChoice.FPS_60
                                "90 fps" -> FpsChoice.FPS_90
                                "120 fps" -> FpsChoice.FPS_120
                                else -> FpsChoice.KEEP
                            }
                            onChange(settings.copy(fps = fps))
                        },
                    )
                    AuroraDropdownRow(
                        label = "Encoding",
                        value = settings.preset.name.lowercase().replaceFirstChar { it.uppercase() },
                        options = EncodingPreset.entries.map {
                            it.name.lowercase().replaceFirstChar { c -> c.uppercase() }
                        },
                        onSelect = {
                            onChange(settings.copy(preset = EncodingPreset.valueOf(it.uppercase())))
                        },
                    )
                    AuroraDropdownRow(
                        label = "Profile",
                        value = settings.profile.name,
                        options = H264Profile.entries.map { it.name },
                        onSelect = { onChange(settings.copy(profile = H264Profile.valueOf(it))) },
                    )
                    AuroraDropdownRow(
                        label = "GOP",
                        value = "${settings.gopSeconds}s",
                        options = listOf("1s", "2s", "5s", "10s"),
                        onSelect = { onChange(settings.copy(gopSeconds = it.replace("s", "").toInt())) },
                    )
                }
            }
        }
    }
}

private fun qualityLabel(crf: Int): String = when {
    crf <= 18 -> "Lossless"
    crf <= 23 -> "High quality"
    crf <= 28 -> "Medium"
    crf <= 35 -> "Low size"
    else -> "Smallest"
}
