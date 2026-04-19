package com.videoeditor.feature.compress.model

enum class VideoCodec { H264, H265 }
enum class RateControl { CRF, CBR, VBR }
enum class EncodingPreset { ULTRAFAST, SUPERFAST, VERYFAST, FAST, MEDIUM, SLOW, VERYSLOW }
enum class H264Profile { BASELINE, MAIN, HIGH }
enum class FpsChoice { KEEP, FPS_24, FPS_30, FPS_60 }
enum class AudioChannels { MONO, STEREO }

enum class ResolutionPreset(val shortEdgePx: Int, val label: String) {
    P480(480, "480p"), P720(720, "720p"), P1080(1080, "1080p"),
    P1440(1440, "1440p"), P2160(2160, "2160p (4K)"), P4320(4320, "4320p (8K)");
    companion object { fun keep() = null }
}

data class CompressionSettings(
    val codec: VideoCodec = VideoCodec.H264,
    val rateControl: RateControl = RateControl.CRF,
    val crf: Int = 23,
    val targetBitrateKbps: Int = 2_500,
    val fps: FpsChoice = FpsChoice.KEEP,
    val resolution: ResolutionPreset? = null,
    val preset: EncodingPreset = EncodingPreset.MEDIUM,
    val profile: H264Profile = H264Profile.HIGH,
    val gopSeconds: Int = 2,
    val useHardwareAccel: Boolean = false,
    val audio: AudioSettings = AudioSettings(),
)

data class AudioSettings(
    val bitrateKbps: Int = 128,
    val channels: AudioChannels = AudioChannels.STEREO,
)