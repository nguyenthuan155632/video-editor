package com.videoeditor.core.ffmpeg

import com.videoeditor.feature.compress.model.EncodeProgress
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProgressParser @Inject constructor() {

    fun parse(line: CharSequence, totalDurationMs: Long): EncodeProgress? {
        var outTimeUs: Long? = null
        var frame: Long? = null
        var fps: Double? = null

        line.lines().forEach { l ->
            val idx = l.indexOf('=')
            if (idx > 0) {
                val key = l.substring(0, idx).trim()
                val value = l.substring(idx + 1).trim()
                when (key) {
                    "out_time_us" -> outTimeUs = value.toLongOrNull()
                    "frame" -> frame = value.toLongOrNull()
                    "fps" -> fps = value.toDoubleOrNull()
                }
            }
        }

        if (outTimeUs == null && frame == null) return null

        val percent = if (totalDurationMs > 0 && outTimeUs != null) {
            ((outTimeUs.toDouble() / 1000.0) / totalDurationMs).toFloat().coerceIn(0f, 1f)
        } else 0f

        val etaSeconds = if (fps != null && fps!! > 0 && frame != null && totalDurationMs > 0) {
            val totalFrames = (totalDurationMs / 1000.0 * fps!!).toLong()
            ((totalFrames - frame!!) / fps!!).toLong().coerceAtLeast(0)
        } else null

        return EncodeProgress(
            percent = percent,
            frame = frame ?: 0,
            fps = fps ?: 0.0,
            etaSeconds = etaSeconds,
            bitrateKbps = null,
        )
    }
}