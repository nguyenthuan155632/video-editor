package com.videoeditor.feature.compress.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.workDataOf
import com.videoeditor.core.estimator.OutputSizeEstimator
import com.videoeditor.core.ffmpeg.FFmpegRunner
import com.videoeditor.core.ffmpeg.RunResult
import com.videoeditor.core.probe.ProbeResult
import com.videoeditor.core.probe.VideoProbe
import com.videoeditor.core.storage.MediaStoreSaver
import com.videoeditor.core.storage.ScopedTempDir
import com.videoeditor.feature.compress.model.CompressionSettings
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

class CompressWorkLauncher @Inject constructor(
    @ApplicationContext private val ctx: Context,
    private val ffmpegRunner: FFmpegRunner,
    private val videoProbe: VideoProbe,
    private val estimator: OutputSizeEstimator,
    private val mediaStoreSaver: MediaStoreSaver,
    private val scopedTempDir: ScopedTempDir,
) {
    suspend fun launch(
        uri: android.net.Uri,
        settings: CompressionSettings,
        onProgress: (Float) -> Unit,
        onComplete: (android.net.Uri, Boolean) -> Unit,
        onError: (String) -> Unit,
    ) {
        val cachedFile = scopedTempDir.copyToCache(uri)
        val workDir = scopedTempDir.createWorkDir()
        val outputFile = java.io.File(workDir, "output.mp4")

        val source = videoProbe.probe(uri)

        ffmpegRunner.runEncode(
            source = source,
            settings = settings,
            inputPath = cachedFile.absolutePath,
            outputPath = outputFile.absolutePath,
            totalDurationMs = source.durationMs,
        ).onEach { update ->
            onProgress(update.progress.percent)
        }.collect { result ->
            scopedTempDir.cleanup(workDir)
            scopedTempDir.cleanup(cachedFile.parentFile)

            when (result) {
                is RunResult.Success -> {
                    val savedUri = mediaStoreSaver.saveToGallery(outputFile, source.displayName)
                    onComplete(savedUri, false)
                }
                is RunResult.Cancelled -> {
                    onError("Compression cancelled")
                }
                is RunResult.Failed -> {
                    onError(result.reason)
                }
            }
        }
    }

    fun cleanup() {
        scopedTempDir.cleanupAll()
    }
}