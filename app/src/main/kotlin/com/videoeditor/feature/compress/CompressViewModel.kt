package com.videoeditor.feature.compress

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.videoeditor.core.estimator.OutputSizeEstimator
import com.videoeditor.core.probe.ProbeResult
import com.videoeditor.core.probe.VideoProbe
import com.videoeditor.feature.compress.model.CompressUiState
import com.videoeditor.feature.compress.model.CompressionSettings
import com.videoeditor.feature.compress.model.EncodeProgress
import com.videoeditor.feature.compress.model.SectionId
import com.videoeditor.feature.compress.model.SmartPreset
import com.videoeditor.feature.compress.work.CompressWorkLauncher
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class CompressViewModel @Inject constructor(
    private val videoProbe: VideoProbe,
    private val estimator: OutputSizeEstimator,
    private val workLauncher: CompressWorkLauncher,
) : ViewModel() {

    private val _uiState = MutableStateFlow<CompressUiState>(CompressUiState.Idle)
    val uiState: StateFlow<CompressUiState> = _uiState.asStateFlow()

    fun onPickVideo() {
        _uiState.update { CompressUiState.PickingVideo }
    }

    fun onVideoSelected(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { CompressUiState.Idle }
            try {
                val probeResult = videoProbe.probe(uri)
                val settings = CompressionSettings()
                val estimate = estimator.estimate(probeResult, settings)
                _uiState.update {
                    CompressUiState.Configuring(
                        source = probeResult,
                        settings = settings,
                        activeSmartPreset = SmartPreset.BALANCED,
                        estimate = estimate,
                        expandedSections = setOf(SectionId.VIDEO, SectionId.AUDIO),
                    )
                }
            } catch (e: Exception) {
                _uiState.update { CompressUiState.Failed(null, e.message ?: "Failed to probe video") }
            }
        }
    }

    fun onSmartPresetSelected(preset: SmartPreset) {
        val current = (_uiState.value as? CompressUiState.Configuring) ?: return
        val newSettings = preset.apply(current.source)
        val estimate = estimator.estimate(current.source, newSettings)
        _uiState.update {
            current.copy(
                settings = newSettings,
                activeSmartPreset = preset,
                estimate = estimate,
            )
        }
    }

    fun onSettingsChanged(settings: CompressionSettings) {
        val current = (_uiState.value as? CompressUiState.Configuring) ?: return
        val estimate = estimator.estimate(current.source, settings)
        _uiState.update {
            current.copy(
                settings = settings,
                activeSmartPreset = null,
                estimate = estimate,
            )
        }
    }

    fun onSectionToggle(section: SectionId) {
        val current = (_uiState.value as? CompressUiState.Configuring) ?: return
        val expanded = current.expandedSections.toMutableSet()
        if (expanded.contains(section)) {
            expanded.remove(section)
        } else {
            expanded.add(section)
        }
        _uiState.update { current.copy(expandedSections = expanded) }
    }

    fun onStartEncode() {
        val config = (_uiState.value as? CompressUiState.Configuring) ?: return
        _uiState.update {
            CompressUiState.Running(
                source = config.source,
                workId = UUID.randomUUID(),
                progress = EncodeProgress(),
                isPaused = false,
            )
        }
        viewModelScope.launch {
            workLauncher.launch(
                uri = config.source.uri,
                settings = config.settings,
                onProgress = { encodeProgress ->
                    _uiState.update { state ->
                        if (state is CompressUiState.Running) {
                            state.copy(progress = encodeProgress)
                        } else state
                    }
                },
                onComplete = { uri, sizeBytes, usedFallback ->
                    val ratio = if (config.source.sizeBytes > 0) {
                        sizeBytes.toDouble() / config.source.sizeBytes
                    } else 0.0
                    _uiState.update {
                        CompressUiState.Done(
                            source = config.source,
                            output = com.videoeditor.feature.compress.model.SavedOutput(
                                uri = uri,
                                displayName = config.source.displayName,
                                sizeBytes = sizeBytes,
                                usedHardwareFallback = usedFallback,
                            ),
                            ratio = ratio,
                        )
                    }
                },
                onError = { reason ->
                    _uiState.update {
                        CompressUiState.Failed(config.source, reason)
                    }
                },
            )
        }
    }

    fun onCancelEncode() {
        workLauncher.cancel()
        _uiState.update { CompressUiState.Idle }
    }

    fun onBack() {
        _uiState.update { CompressUiState.Idle }
    }

    fun onDismissError() {
        _uiState.update { CompressUiState.Idle }
    }
}