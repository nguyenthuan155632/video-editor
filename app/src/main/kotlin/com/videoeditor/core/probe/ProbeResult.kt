package com.videoeditor.core.probe

import android.net.Uri

data class ProbeResult(
    val uri: Uri,
    val displayName: String,
    val durationMs: Long,
    val widthPx: Int,
    val heightPx: Int,
    val frameRate: Double,
    val videoBitrateBps: Long,
    val videoCodec: String,
    val audioCodec: String?,
    val audioChannels: Int?,
    val sizeBytes: Long,
    val rotationDegrees: Int,
) {
    fun shortEdge(): Int = minOf(widthPx, heightPx)
}