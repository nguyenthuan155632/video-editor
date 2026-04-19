package com.videoeditor.feature.compress.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.videoeditor.core.theme.AuroraBgBase
import com.videoeditor.feature.compress.model.CompressUiState
import com.videoeditor.feature.compress.model.CompressionSettings
import com.videoeditor.feature.compress.model.SectionId
import com.videoeditor.feature.compress.model.SmartPreset
import com.videoeditor.feature.compress.ui.configuring.AdvancedSection
import com.videoeditor.feature.compress.ui.configuring.AudioSection
import com.videoeditor.feature.compress.ui.configuring.OutputEstimateBar
import com.videoeditor.feature.compress.ui.configuring.ResolutionPresetRow
import com.videoeditor.feature.compress.ui.configuring.VideoHeroCard
import com.videoeditor.feature.compress.ui.configuring.VideoSection

@Composable
fun ConfiguringStep(
    state: CompressUiState.Configuring,
    onPickDifferent: () -> Unit,
    onSmartPreset: (SmartPreset) -> Unit,
    onSettingsChanged: (CompressionSettings) -> Unit,
    onSectionToggle: (SectionId) -> Unit,
    onStartEncode: () -> Unit,
) {
    @Suppress("UNUSED_PARAMETER") val _smart = onSmartPreset
    @Suppress("UNUSED_PARAMETER") val _pick = onPickDifferent

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 160.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            VideoHeroCard(source = state.source)
            ResolutionPresetRow(
                selected = state.settings.resolution,
                sourceHeight = state.source.heightPx,
                onSelect = { onSettingsChanged(state.settings.copy(resolution = it)) },
            )
            VideoSection(
                settings = state.settings,
                expanded = state.expandedSections.contains(SectionId.VIDEO),
                onToggle = { onSectionToggle(SectionId.VIDEO) },
                onChange = onSettingsChanged,
            )
            AudioSection(
                settings = state.settings,
                expanded = state.expandedSections.contains(SectionId.AUDIO),
                onToggle = { onSectionToggle(SectionId.AUDIO) },
                onChange = onSettingsChanged,
            )
            AdvancedSection(
                settings = state.settings,
                expanded = state.expandedSections.contains(SectionId.ADVANCED),
                onToggle = { onSectionToggle(SectionId.ADVANCED) },
                onChange = onSettingsChanged,
            )
        }
        // Gradient scrim — fades content into the bar so the overlap is never jarring
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(100.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(AuroraBgBase.copy(alpha = 0f), AuroraBgBase.copy(alpha = 0.95f)),
                    ),
                ),
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
        ) {
            OutputEstimateBar(
                estimate = state.estimate,
                sourceSizeBytes = state.source.sizeBytes,
                onCompress = onStartEncode,
            )
        }
    }
}
