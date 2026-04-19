package com.videoeditor.feature.compress.work

import android.content.Context
import com.videoeditor.core.estimator.OutputSizeEstimator
import com.videoeditor.core.ffmpeg.FFmpegCommandBuilder
import com.videoeditor.core.ffmpeg.FFmpegRunner
import com.videoeditor.core.ffmpeg.RunResult
import com.videoeditor.core.probe.VideoProbe
import com.videoeditor.core.storage.MediaStoreSaver
import com.videoeditor.core.storage.ScopedTempDir
import com.videoeditor.feature.compress.model.CompressionSettings
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

class CompressWorkLauncher @Inject constructor(
    @ApplicationContext private val ctx: Context,
    private val ffmpegRunner: FFmpegRunner,
    private val videoProbe: VideoProbe,
    private val estimator: OutputSizeEstimator,
    private val mediaStoreSaver: MediaStoreSaver,
    private val scopedTempDir: ScopedTempDir,
) {
    private var currentJob: Job? = null

    fun launch(
        uri: android.net.Uri,
        settings: CompressionSettings,
        onProgress: (Float) -> Unit,
        onComplete: (android.net.Uri, Boolean) -> Unit,
        onError: (String) -> Unit,
    ) {
        currentJob?.cancel()
        currentJob = kotlinx.coroutines.GlobalScope.launch {
            coroutineScope {
                launch {
                    var progress = 0f
                    while (progress < 0.99f) {
                        delay(500)
                        if (progress < 0.95f) {
                            progress += 0.02f
                            onProgress(progress.coerceAtMost(0.95f))
                        }
                    }
                }

                val result = withContext(Dispatchers.IO) {
                    val cachedFile = scopedTempDir.copyToCache(uri)
                    val workDir = scopedTempDir.createWorkDir()
                    val outputFile = java.io.File(workDir, "output.mp4")

                    try {
                        val source = videoProbe.probe(uri)
                        val probeResult = ffmpegRunner.execute(
                            source = source,
                            settings = settings,
                            inputPath = cachedFile.absolutePath,
                            outputPath = outputFile.absolutePath,
                            totalDurationMs = source.durationMs,
                        )

                        when (probeResult) {
                            is RunResult.Success -> {
                                val savedUri = mediaStoreSaver.saveToGallery(outputFile, source.displayName)
                                scopedTempDir.cleanup(workDir)
                                scopedTempDir.cleanup(cachedFile.parentFile!!)
                                onProgress(1f)
                                onComplete(savedUri, probeResult.usedHardwareFallback)
                            }
                            is RunResult.Cancelled -> {
                                scopedTempDir.cleanup(workDir)
                                scopedTempDir.cleanup(cachedFile.parentFile!!)
                                onError("Compression cancelled")
                            }
                            is RunResult.Failed -> {
                                scopedTempDir.cleanup(workDir)
                                scopedTempDir.cleanup(cachedFile.parentFile!!)
                                onError(probeResult.reason)
                            }
                        }
                    } catch (e: Exception) {
                        scopedTempDir.cleanup(workDir)
                        onError(e.message ?: "Unknown error")
                    }
                }
            }
        }
    }

    fun cancel() {
        currentJob?.cancel()
        ffmpegRunner.cancel()
        scopedTempDir.cleanupAll()
    }
}