package com.videoeditor.feature.compress.ui.configuring

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.videoeditor.core.designsys.GlassCard
import com.videoeditor.core.designsys.SectionHeader
import com.videoeditor.core.theme.AuroraBorder
import com.videoeditor.core.theme.AuroraCyan
import com.videoeditor.core.theme.AuroraTextPrimary
import com.videoeditor.core.theme.AuroraTextSecondary
import com.videoeditor.feature.compress.model.CompressionSettings

@Composable
fun AdvancedSection(
    settings: CompressionSettings,
    expanded: Boolean,
    onToggle: () -> Unit,
    onChange: (CompressionSettings) -> Unit,
    modifier: Modifier = Modifier,
) {
    GlassCard(modifier = modifier.fillMaxWidth(), onClick = onToggle) {
        Column {
            SectionHeader(
                title = "Advanced",
                summary = if (settings.useHardwareAccel) "Hardware accel on" else "Software only",
            )
            if (expanded) {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Hardware acceleration", color = AuroraTextPrimary)
                        Text(
                            text = if (settings.useHardwareAccel) "Uses HW encoder when available"
                            else "Software encoding (libx264/libx265)",
                            style = MaterialTheme.typography.bodySmall,
                            color = AuroraTextSecondary,
                        )
                    }
                    Switch(
                        checked = settings.useHardwareAccel,
                        onCheckedChange = { onChange(settings.copy(useHardwareAccel = it)) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = AuroraCyan,
                            checkedTrackColor = AuroraCyan.copy(alpha = 0.5f),
                            uncheckedThumbColor = AuroraTextSecondary,
                            uncheckedTrackColor = AuroraBorder,
                        ),
                    )
                }
            }
        }
    }
}
