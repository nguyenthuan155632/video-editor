package com.videoeditor.feature.compress.ui

import android.content.Intent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.videoeditor.core.designsys.GradientButton
import com.videoeditor.core.designsys.GradientButtonVariant
import com.videoeditor.core.designsys.GradientIconBadge
import com.videoeditor.core.designsys.StatPill
import com.videoeditor.core.theme.AuroraGradients
import com.videoeditor.core.theme.AuroraTextSecondary
import com.videoeditor.feature.compress.model.SavedOutput
import java.text.DecimalFormat

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DoneStep(
    output: SavedOutput,
    ratio: Double,
) {
    val context = LocalContext.current
    val df = DecimalFormat("#.#")
    val savedPercent = ((1.0 - 1.0 / ratio.coerceAtLeast(1.0001)) * 100).toFloat().coerceIn(0f, 99f)
    val gradientH = remember { AuroraGradients.horizontal() }
    val gradientD = remember { AuroraGradients.diagonal() }

    var animateTo by remember { mutableStateOf(0f) }
    LaunchedEffect(savedPercent) { animateTo = savedPercent }
    val animated by animateFloatAsState(targetValue = animateTo, label = "saved-count-up")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        GradientIconBadge(icon = Icons.Default.Check, size = 84.dp)
        Text(
            text = "Compressed",
            style = MaterialTheme.typography.displayMedium.copy(brush = gradientH),
            textAlign = TextAlign.Center,
        )
        Text(
            text = "Saved ${animated.toInt()}%",
            style = MaterialTheme.typography.displayLarge.copy(brush = gradientD),
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(8.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            val newMb = output.sizeBytes / (1024.0 * 1024.0)
            val originalMb = newMb * ratio
            StatPill(label = "Original", value = "${df.format(originalMb)} MB")
            StatPill(label = "New", value = "${df.format(newMb)} MB")
        }
        Spacer(modifier = Modifier.weight(1f))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            GradientButton(
                text = "Open",
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(output.uri, "video/*")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(intent)
                },
                variant = GradientButtonVariant.Filled,
                modifier = Modifier.weight(1f),
            )
            GradientButton(
                text = "Share",
                onClick = {
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "video/*"
                        putExtra(Intent.EXTRA_STREAM, output.uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Share video"))
                },
                variant = GradientButtonVariant.Ghost,
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Saved to Photos › VideoEditor",
            style = MaterialTheme.typography.bodySmall,
            color = AuroraTextSecondary,
        )
        Spacer(modifier = Modifier.height(16.dp))
    }
}
