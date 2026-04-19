package com.videoeditor.feature.compress.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun PickStep(
    onPick: () -> Unit,
) {
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
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .border(
                        width = 2.dp,
                        color = Color(0xFF3A3A42),
                        shape = RoundedCornerShape(24.dp),
                    )
                    .background(Color(0xFF1A1A1F))
                    .padding(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.VideoFile,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = Color(0xFF5B6CFF),
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Drop video here",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "or select from your device",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFB0B0B8),
            )
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = onPick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF5B6CFF),
                ),
                shape = RoundedCornerShape(16.dp),
            ) {
                Text("Select video")
            }
        }
    }
}