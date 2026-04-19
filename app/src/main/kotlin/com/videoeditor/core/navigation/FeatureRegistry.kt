package com.videoeditor.core.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AudioFile
import androidx.compose.material.icons.outlined.Compress
import androidx.compose.material.icons.outlined.ContentCut
import androidx.compose.material.icons.outlined.PhotoSizeSelectLarge
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.ui.graphics.vector.ImageVector

data class FeatureCard(
    val id: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val route: String?,
)

object FeatureRegistry {
    val cards: List<FeatureCard> = listOf(
        FeatureCard("compress", "Compress", "Shrink size", Icons.Outlined.Compress, Routes.COMPRESS),
        FeatureCard("trim", "Trim", "Coming soon", Icons.Outlined.ContentCut, route = null),
        FeatureCard("convert", "Convert", "Coming soon", Icons.Outlined.SwapHoriz, route = null),
        FeatureCard("audio", "Audio", "Coming soon", Icons.Outlined.AudioFile, route = null),
        FeatureCard("resize", "Resize", "Coming soon", Icons.Outlined.PhotoSizeSelectLarge, route = null),
    )
}