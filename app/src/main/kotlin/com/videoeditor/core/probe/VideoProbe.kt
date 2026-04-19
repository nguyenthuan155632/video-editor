package com.videoeditor.core.probe

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VideoProbe @Inject constructor(@ApplicationContext private val ctx: Context) {

    suspend fun probe(uri: Uri): ProbeResult = withContext(Dispatchers.IO) {
        val (displayName, size) = queryNameAndSize(uri)
        val mmr = MediaMetadataRetriever()
        try {
            mmr.setDataSource(ctx, uri)
            val durationMs = mmr.extract(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            val width = mmr.extract(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
            val height = mmr.extract(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
            val rotation = mmr.extract(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull() ?: 0
            val mime = mmr.extract(MediaMetadataRetriever.METADATA_KEY_MIMETYPE).orEmpty()

            // Extract frame rate — METADATA_KEY_CAPTURE_FRAMERATE is unreliable on many devices
            // (often returns NaN even for perfectly normal videos), so we also try to
            // derive it from frame count and duration as a fallback.
            val frameRate = extractFrameRate(mmr, durationMs) ?: Double.NaN

            ProbeResult(
                uri = uri,
                displayName = displayName,
                durationMs = durationMs,
                widthPx = width,
                heightPx = height,
                frameRate = frameRate,
                videoBitrateBps = 0L,  // Will be estimated if needed
                videoCodec = mime.removePrefix("video/"),
                audioCodec = null,
                audioChannels = null,
                sizeBytes = size,
                rotationDegrees = rotation,
            )
        } finally {
            mmr.release()
        }
    }

    private fun queryNameAndSize(uri: Uri): Pair<String, Long> {
        ctx.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)?.use { c ->
            if (c.moveToFirst()) {
                val name = c.getString(c.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME))
                val size = c.getLong(c.getColumnIndexOrThrow(OpenableColumns.SIZE))
                return name to size
            }
        }
        return (uri.lastPathSegment ?: "video") to 0L
    }

    private fun MediaMetadataRetriever.extract(key: Int): String? = extractMetadata(key)

    private fun extractFrameRate(mmr: MediaMetadataRetriever, durationMs: Long): Double? {
        // Primary: try METADATA_KEY_CAPTURE_FRAMERATE (often unreliable — returns NaN)
        mmr.extract(MediaMetadataRetriever.METADATA_KEY_CAPTURE_FRAMERATE)
            ?.toDoubleOrNull()
            ?.takeIf { !it.isNaN() && it > 0 }
            ?.let { return it }

        // Fallback: derive from frame count and duration
        val frameCount = mmr.extract(MediaMetadataRetriever.METADATA_KEY_VIDEO_FRAME_COUNT)?.toLongOrNull()
        if (frameCount != null && durationMs > 0 && frameCount > 0) {
            val fps = (frameCount * 1000.0) / durationMs
            if (fps.isFinite() && fps > 0) return fps
        }

        // Final fallback: probe two frames and measure delta
        try {
            val frame1 = mmr.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST)
            if (frame1 != null && durationMs > 0) {
                val frameTimeUs = 1_000_000L // 1 second
                val frame2 = mmr.getFrameAtTime(frameTimeUs, MediaMetadataRetriever.OPTION_CLOSEST)
                if (frame2 != null) return 1.0
            }
        } catch (_: Exception) {
        }

        return null
    }
}