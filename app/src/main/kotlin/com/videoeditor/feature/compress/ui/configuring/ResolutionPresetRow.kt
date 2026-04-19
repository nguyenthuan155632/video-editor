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
import com.videoeditor.core.designsys.AuroraChip
import com.videoeditor.core.theme.AuroraTextPrimary
import com.videoeditor.feature.compress.model.ResolutionPreset

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ResolutionPresetRow(
    selected: ResolutionPreset?,
    sourceHeight: Int,
    onSelect: (ResolutionPreset?) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Resolution",
            style = MaterialTheme.typography.titleMedium,
            color = AuroraTextPrimary,
        )
        Spacer(modifier = Modifier.height(12.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ResolutionPreset.entries.forEach { preset ->
                val disabled = preset.shortEdgePx > sourceHeight
                AuroraChip(
                    label = preset.label,
                    selected = selected == preset,
                    enabled = !disabled,
                    onClick = { onSelect(if (selected == preset) null else preset) },
                )
            }
            AuroraChip(
                label = "Keep",
                selected = selected == null,
                onClick = { onSelect(null) },
            )
        }
    }
}
