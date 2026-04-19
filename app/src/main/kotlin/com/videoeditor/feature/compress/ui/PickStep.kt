package com.videoeditor.feature.compress.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.videoeditor.core.designsys.GradientButton
import com.videoeditor.core.designsys.GradientButtonVariant
import com.videoeditor.core.designsys.GradientIconBadge
import com.videoeditor.core.theme.AuroraGradients
import com.videoeditor.core.theme.AuroraTextSecondary

@Composable
fun PickStep(onPick: () -> Unit) {
    val gradient = remember { AuroraGradients.horizontal() }
    Box(
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth(),
        ) {
            GradientIconBadge(
                icon = Icons.Default.VideoFile,
                size = 96.dp,
            )
            Spacer(modifier = Modifier.height(28.dp))
            Text(
                text = "Pick a video",
                style = MaterialTheme.typography.displayMedium.copy(brush = gradient),
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "MP4, MOV, MKV, WebM — anything Android can read.",
                style = MaterialTheme.typography.bodyMedium,
                color = AuroraTextSecondary,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(32.dp))
            GradientButton(
                text = "Choose video",
                onClick = onPick,
                variant = GradientButtonVariant.Filled,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
