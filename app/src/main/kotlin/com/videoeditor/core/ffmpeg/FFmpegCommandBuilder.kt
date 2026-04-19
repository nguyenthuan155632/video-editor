package com.videoeditor.core.ffmpeg

import com.videoeditor.core.probe.ProbeResult
import com.videoeditor.feature.compress.model.AudioChannels
import com.videoeditor.feature.compress.model.CompressionSettings
import com.videoeditor.feature.compress.model.EncodingPreset
import com.videoeditor.feature.compress.model.FpsChoice
import com.videoeditor.feature.compress.model.H264Profile
import com.videoeditor.feature.compress.model.RateControl
import com.videoeditor.feature.compress.model.ResolutionPreset
import com.videoeditor.feature.compress.model.VideoCodec
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FFmpegCommandBuilder @Inject constructor() {

    fun build(
        source: ProbeResult,
        settings: CompressionSettings,
        input: String,
        output: String,
        useHwEncoder: Boolean = settings.useHardwareAccel,
    ): Array<String> {
        val args = mutableListOf("-y", "-hide_banner", "-loglevel", "warning", "-i", input)

        val effectiveFps = effectiveFps(source, settings.fps)
        val resolution = effectiveResolution(source, settings.resolution)

        if (resolution != null) {
            val isLandscape = source.widthPx >= source.heightPx
            val scale = if (isLandscape)
                "scale=-2:${resolution.shortEdgePx}:flags=lanczos"
            else
                "scale=${resolution.shortEdgePx}:-2:flags=lanczos"
            args += listOf("-vf", scale)
        }

        args += videoCodecArgs(settings.codec, useHwEncoder)

        if (!useHwEncoder) {
            args += listOf("-preset", settings.preset.cli())
        }

        when (settings.codec) {
            VideoCodec.H264 -> {
                args += listOf("-profile:v", settings.profile.cli())
                args += listOf("-level", h264Level(resolution?.shortEdgePx ?: source.shortEdge(), effectiveFps))
            }
            VideoCodec.H265 -> {
                args += listOf("-profile:v", "main")
                args += listOf("-level", h265Level(resolution?.shortEdgePx ?: source.shortEdge(), effectiveFps))
                args += listOf("-tag:v", "hvc1")
            }
        }

        when (settings.rateControl) {
            RateControl.CRF -> {
                if (useHwEncoder) {
                    val kbps = crfToBitrateKbps(settings.crf, resolution?.shortEdgePx ?: source.shortEdge(), effectiveFps)
                    args += listOf("-b:v", "${kbps}k")
                } else {
                    args += listOf("-crf", settings.crf.toString())
                }
            }
            RateControl.CBR -> {
                val kbps = settings.targetBitrateKbps
                args += listOf("-b:v", "${kbps}k", "-minrate", "${kbps}k",
                               "-maxrate", "${kbps}k", "-bufsize", "${kbps * 2}k")
                if (!useHwEncoder) {
                    args += when (settings.codec) {
                        VideoCodec.H264 -> listOf("-x264-params", "nal-hrd=cbr")
                        VideoCodec.H265 -> listOf("-x265-params", "strict-cbr=1")
                    }
                }
            }
            RateControl.VBR -> {
                val kbps = settings.targetBitrateKbps
                val max = (kbps * 1.5).toInt()
                args += listOf("-b:v", "${kbps}k", "-maxrate", "${max}k", "-bufsize", "${kbps * 2}k")
            }
        }

        if (settings.fps != FpsChoice.KEEP) {
            args += listOf("-r", effectiveFps.toString())
        }

        val gopFrames = (effectiveFps * settings.gopSeconds).coerceAtLeast(1)
        args += listOf("-g", gopFrames.toString(), "-keyint_min", gopFrames.toString())
        if (settings.rateControl == RateControl.CBR) args += listOf("-sc_threshold", "0")

        if (source.audioCodec == null) {
            args += "-an"
        } else {
            args += listOf("-c:a", "aac",
                           "-b:a", "${settings.audio.bitrateKbps}k",
                           "-ac", if (settings.audio.channels == AudioChannels.MONO) "1" else "2")
        }

        args += listOf("-movflags", "+faststart", "-progress", "pipe:1", "-nostats", output)

        return args.toTypedArray()
    }

    private fun videoCodecArgs(codec: VideoCodec, hw: Boolean): List<String> = when {
        codec == VideoCodec.H264 && hw -> listOf("-c:v", "h264_mediacodec")
        codec == VideoCodec.H264 && !hw -> listOf("-c:v", "libx264")
        codec == VideoCodec.H265 && hw -> listOf("-c:v", "hevc_mediacodec")
        codec == VideoCodec.H265 && !hw -> listOf("-c:v", "libx265")
        else -> error("unreachable")
    }

    private fun effectiveFps(source: ProbeResult, fps: FpsChoice): Int {
        val src = if (source.frameRate.isNaN() || source.frameRate <= 0) 30.0 else source.frameRate
        val target = when (fps) {
            FpsChoice.KEEP -> src.toInt().coerceAtLeast(1)
            FpsChoice.FPS_24 -> 24
            FpsChoice.FPS_30 -> 30
            FpsChoice.FPS_60 -> 60
        }
        return target.coerceAtMost(src.toInt().coerceAtLeast(1))
    }

    private fun effectiveResolution(source: ProbeResult, target: ResolutionPreset?): ResolutionPreset? {
        target ?: return null
        return if (target.shortEdgePx >= source.shortEdge()) null else target
    }

    private fun h264Level(shortEdgePx: Int, fps: Int): String = when {
        shortEdgePx <= 1080 && fps <= 30 -> "4.1"
        shortEdgePx <= 2160 && fps <= 30 -> "5.1"
        shortEdgePx <= 2160 && fps <= 60 -> "5.2"
        else -> "6.2"
    }

    private fun h265Level(shortEdgePx: Int, fps: Int): String = when {
        shortEdgePx <= 1080 && fps <= 30 -> "4"
        shortEdgePx <= 2160 && fps <= 30 -> "5"
        shortEdgePx <= 2160 && fps <= 60 -> "5.1"
        else -> "6.2"
    }

    private fun crfToBitrateKbps(crf: Int, shortEdgePx: Int, fps: Int): Int {
        val base = when {
            shortEdgePx <= 480 -> 800
            shortEdgePx <= 720 -> 1_800
            shortEdgePx <= 1080 -> 4_500
            shortEdgePx <= 1440 -> 9_000
            shortEdgePx <= 2160 -> 22_000
            else -> 60_000
        }
        val crfFactor = Math.pow(2.0, (23 - crf) / 6.0)
        val fpsFactor = fps / 30.0
        return (base * crfFactor * fpsFactor).toInt().coerceIn(200, 200_000)
    }

    private fun ProbeResult.shortEdge() = minOf(widthPx, heightPx)

    private fun EncodingPreset.cli() = name.lowercase()
    private fun H264Profile.cli() = name.lowercase()
}