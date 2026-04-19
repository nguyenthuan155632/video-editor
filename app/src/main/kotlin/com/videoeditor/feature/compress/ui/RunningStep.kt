package com.videoeditor.feature.compress.ui

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.BorderStroke
import com.videoeditor.feature.compress.model.EncodeProgress
import java.text.DecimalFormat

@Composable
fun RunningStep(
    progress: EncodeProgress,
    onCancel: () -> Unit,
) {
    val df = DecimalFormat("#.#")

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D0F))
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = "Compressing…",
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
            )
            Spacer(modifier = Modifier.height(32.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFF3A3A42)),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress.percent.toFloat())
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFF5B6CFF)),
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "${(progress.percent * 100).toInt()}%",
                style = MaterialTheme.typography.headlineMedium,
                color = Color(0xFF5B6CFF),
            )
            if (progress.etaSeconds != null) {
                val mins = progress.etaSeconds / 60
                val secs = progress.etaSeconds % 60
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "ETA ${mins}:${secs.toString().padStart(2, '0')}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFB0B0B8),
                )
                Text(
                    text = "frame ${progress.frame} · ${df.format(progress.fps)} fps",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFB0B0B8),
                )
            }
            Spacer(modifier = Modifier.height(48.dp))
            OutlinedButton(
                onClick = onCancel,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color(0xFFB0B0B8),
                ),
                border = BorderStroke(1.dp, Color(0xFF3A3A42)),
                shape = RoundedCornerShape(12.dp),
            ) {
                Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                Text("Cancel", modifier = Modifier.padding(start = 8.dp))
            }
        }
    }
}