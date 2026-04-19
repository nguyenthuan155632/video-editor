package com.videoeditor.core.estimator

import com.videoeditor.core.probe.ProbeResult
import com.videoeditor.feature.compress.model.CompressionSettings
import com.videoeditor.feature.compress.model.FpsChoice
import com.videoeditor.feature.compress.model.OutputEstimate
import com.videoeditor.feature.compress.model.RateControl
import com.videoeditor.feature.compress.model.ResolutionPreset
import com.videoeditor.feature.compress.model.VideoCodec
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max
import kotlin.math.roundToInt

@Singleton
class OutputSizeEstimator @Inject constructor() {

    fun estimate(source: ProbeResult, s: CompressionSettings): OutputEstimate {
        val effectiveFps = effectiveFps(source, s.fps)
        val effectiveRes = effectiveResolution(source, s.resolution)
        val shortEdge = effectiveRes?.shortEdgePx ?: minOf(source.widthPx, source.heightPx)

        val videoKbps = when (s.rateControl) {
            RateControl.CBR, RateControl.VBR -> s.targetBitrateKbps
            RateControl.CRF -> crfToBitrate(s.crf, s.codec, shortEdge, effectiveFps)
        }
        val audioKbps = if (source.audioCodec != null) s.audio.bitrateKbps else 0
        val totalKbps = videoKbps + audioKbps
        val durSec = source.durationMs / 1000.0
        val sizeBytes = (totalKbps * 1000.0 / 8.0 * durSec).toLong() + 100_000

        val ratio = if (source.sizeBytes > 0) sizeBytes.toDouble() / source.sizeBytes else 0.0
        val notes = buildList {
            if (s.useHardwareAccel) add("Hardware encoder will be used (CRF mapped to bitrate).")
            if (effectiveRes == null && s.resolution != null) add("Original resolution kept (no upscaling).")
        }
        return OutputEstimate(sizeBytes, ratio, totalKbps, notes)
    }

    private fun crfToBitrate(crf: Int, codec: VideoCodec, shortEdge: Int, fps: Int): Int {
        val base = when {
            shortEdge <= 480 -> 800
            shortEdge <= 720 -> 1_800
            shortEdge <= 1080 -> 4_500
            shortEdge <= 1440 -> 9_000
            shortEdge <= 2160 -> 22_000
            else -> 60_000
        }
        val codecFactor = if (codec == VideoCodec.H265) 0.65 else 1.0
        val crfFactor = Math.pow(2.0, (23 - crf) / 6.0)
        val fpsFactor = fps / 30.0
        return max(200, (base * codecFactor * crfFactor * fpsFactor).toInt())
    }

    private fun effectiveFps(source: ProbeResult, fps: FpsChoice): Int {
        val src = source.frameRate.takeIf { it.isFinite() && it > 0 } ?: 30.0
        val target = when (fps) {
            FpsChoice.KEEP -> src.roundToInt().coerceAtLeast(1)
            FpsChoice.FPS_24 -> 24
            FpsChoice.FPS_30 -> 30
            FpsChoice.FPS_60 -> 60
        }
        return target.coerceAtMost(src.roundToInt().coerceAtLeast(1))
    }

    private fun effectiveResolution(source: ProbeResult, target: ResolutionPreset?): ResolutionPreset? {
        target ?: return null
        return if (target.shortEdgePx >= source.shortEdge()) null else target
    }
}