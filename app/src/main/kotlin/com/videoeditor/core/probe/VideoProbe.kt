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

            // Extract frame rate from MediaMetadataRetriever if available
            val frameRate = try {
                val rate = mmr.extract(MediaMetadataRetriever.METADATA_KEY_CAPTURE_FRAMERATE)
                rate?.toDoubleOrNull() ?: Double.NaN
            } catch (e: Exception) {
                Double.NaN
            }

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
}