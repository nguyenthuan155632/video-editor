package com.videoeditor.feature.compress.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.videoeditor.core.designsys.GlassCard
import com.videoeditor.core.designsys.GradientButton
import com.videoeditor.core.designsys.GradientButtonVariant
import com.videoeditor.core.designsys.GradientIconBadge
import com.videoeditor.core.theme.AuroraError
import com.videoeditor.core.theme.AuroraTextPrimary
import com.videoeditor.core.theme.AuroraTextSecondary
import com.videoeditor.core.theme.AuroraWarning

@Composable
fun FailedStep(
    reason: String,
    onDismiss: () -> Unit,
    onRetry: () -> Unit,
) {
    val warningBrush = remember { Brush.linearGradient(listOf(AuroraWarning, AuroraError)) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                GradientIconBadge(
                    icon = Icons.Default.Warning,
                    size = 72.dp,
                    brush = warningBrush,
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Something went wrong",
                    style = MaterialTheme.typography.headlineSmall,
                    color = AuroraTextPrimary,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = reason,
                    style = MaterialTheme.typography.bodyMedium,
                    color = AuroraTextSecondary,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    GradientButton(
                        text = "Try again",
                        onClick = onRetry,
                        variant = GradientButtonVariant.Filled,
                        modifier = Modifier.weight(1f),
                    )
                    GradientButton(
                        text = "Dismiss",
                        onClick = onDismiss,
                        variant = GradientButtonVariant.Ghost,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}
