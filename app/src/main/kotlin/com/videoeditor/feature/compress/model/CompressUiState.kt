package com.videoeditor.feature.compress.model

import android.net.Uri
import com.videoeditor.core.probe.ProbeResult
import java.util.UUID

enum class SectionId { VIDEO, AUDIO, ADVANCED }

data class EncodeProgress(
    val percent: Float = 0f,
    val frame: Long = 0,
    val fps: Double = 0.0,
    val etaSeconds: Long? = null,
    val bitrateKbps: Int? = null,
)

data class SavedOutput(
    val uri: Uri,
    val displayName: String,
    val sizeBytes: Long,
    val usedHardwareFallback: Boolean = false,
)

sealed interface CompressUiState {
    data object Idle : CompressUiState
    data object PickingVideo : CompressUiState
    data class Configuring(
        val source: ProbeResult,
        val settings: CompressionSettings,
        val activeSmartPreset: SmartPreset?,
        val estimate: OutputEstimate,
        val expandedSections: Set<SectionId> = setOf(SectionId.VIDEO, SectionId.AUDIO),
    ) : CompressUiState
    data class Running(
        val source: ProbeResult,
        val workId: UUID,
        val progress: EncodeProgress,
        val isPaused: Boolean = false,
    ) : CompressUiState
    data class Done(
        val source: ProbeResult,
        val output: SavedOutput,
        val ratio: Double,
    ) : CompressUiState
    data class Failed(val source: ProbeResult?, val reason: String) : CompressUiState
}