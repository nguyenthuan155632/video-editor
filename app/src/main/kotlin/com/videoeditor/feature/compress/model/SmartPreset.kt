package com.videoeditor.feature.compress.model

import com.videoeditor.core.probe.ProbeResult

enum class SmartPreset(val label: String) {
    SMALL("Small size"),
    BALANCED("Balanced"),
    HIGH_QUALITY("High quality"),
    SOCIAL("Social media");

    fun apply(source: ProbeResult): CompressionSettings = when (this) {
        SMALL -> CompressionSettings(
            codec = VideoCodec.H265,
            rateControl = RateControl.CRF,
            crf = 30,
            resolution = capResolution(source, ResolutionPreset.P720),
            fps = capFps(source, 30),
            preset = EncodingPreset.MEDIUM,
            audio = AudioSettings(bitrateKbps = 96),
        )
        BALANCED -> CompressionSettings()
        HIGH_QUALITY -> CompressionSettings(
            codec = VideoCodec.H265,
            rateControl = RateControl.CRF,
            crf = 20,
            preset = EncodingPreset.SLOW,
            audio = AudioSettings(bitrateKbps = 192),
        )
        SOCIAL -> CompressionSettings(
            codec = VideoCodec.H264,
            rateControl = RateControl.CBR,
            targetBitrateKbps = 6_000,
            resolution = capResolution(source, ResolutionPreset.P1080),
            fps = capFps(source, 30),
            preset = EncodingPreset.FAST,
            audio = AudioSettings(bitrateKbps = 128),
        )
    }
}

private fun capResolution(source: ProbeResult, target: ResolutionPreset): ResolutionPreset? {
    val srcShort = minOf(source.widthPx, source.heightPx)
    return if (srcShort <= target.shortEdgePx) null else target
}

private fun capFps(source: ProbeResult, target: Int): FpsChoice {
    val src = if (source.frameRate.isNaN() || source.frameRate <= 0) 30.0 else source.frameRate
    return if (src <= target) FpsChoice.KEEP else when (target) {
        24 -> FpsChoice.FPS_24
        30 -> FpsChoice.FPS_30
        60 -> FpsChoice.FPS_60
        else -> FpsChoice.KEEP
    }
}