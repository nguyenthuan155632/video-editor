package com.videoeditor.feature.compress.ui.configuring.internal

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.videoeditor.core.theme.AuroraBorder
import com.videoeditor.core.theme.AuroraGradients
import com.videoeditor.core.theme.AuroraSurface1
import com.videoeditor.core.theme.AuroraSurface2
import com.videoeditor.core.theme.AuroraTextPrimary
import com.videoeditor.core.theme.AuroraTextSecondary
import com.videoeditor.feature.compress.model.AudioChannels
import com.videoeditor.feature.compress.model.RateControl

@Composable
fun AuroraDropdownRow(
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
                .clip(RoundedCornerShape(14.dp))
                .background(AuroraSurface2)
                .border(1.dp, AuroraBorder, RoundedCornerShape(14.dp))
                .clickable { expanded = true }
                .padding(horizontal = 14.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label, color = AuroraTextSecondary, style = MaterialTheme.typography.bodyMedium)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(value, color = AuroraTextPrimary, style = MaterialTheme.typography.titleSmall)
                Spacer(modifier = Modifier.width(8.dp))
                Text("▾", color = AuroraTextSecondary)
            }
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(AuroraSurface1),
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = option,
                            color = if (option == value) AuroraTextPrimary else AuroraTextSecondary,
                            style = MaterialTheme.typography.bodyMedium,
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
fun SegmentedToggleRateControl(selected: RateControl, onSelect: (RateControl) -> Unit) {
    val gradient = remember { AuroraGradients.horizontal() }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        RateControl.entries.forEach { mode ->
            val isSelected = selected == mode
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .then(
                        if (isSelected) Modifier.background(gradient)
                        else Modifier.background(AuroraSurface1),
                    )
                    .border(
                        width = 1.dp,
                        color = if (isSelected) AuroraTextPrimary.copy(alpha = 0f) else AuroraBorder,
                        shape = RoundedCornerShape(14.dp),
                    )
                    .clickable { onSelect(mode) }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = mode.name,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isSelected) AuroraTextPrimary else AuroraTextSecondary,
                )
            }
        }
    }
}

@Composable
fun SegmentedToggleChannels(channels: AudioChannels, onSelect: (AudioChannels) -> Unit) {
    val gradient = remember { AuroraGradients.horizontal() }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        AudioChannels.entries.forEach { ch ->
            val isSelected = channels == ch
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .then(
                        if (isSelected) Modifier.background(gradient)
                        else Modifier.background(AuroraSurface1),
                    )
                    .border(
                        width = 1.dp,
                        color = if (isSelected) AuroraTextPrimary.copy(alpha = 0f) else AuroraBorder,
                        shape = RoundedCornerShape(14.dp),
                    )
                    .clickable { onSelect(ch) }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = ch.name.lowercase().replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isSelected) AuroraTextPrimary else AuroraTextSecondary,
                )
            }
        }
    }
}
