package com.videoeditor.feature.compress.work

import android.content.Context
import com.videoeditor.core.ffmpeg.FFmpegRunner
import com.videoeditor.core.ffmpeg.RunResult
import com.videoeditor.core.probe.VideoProbe
import com.videoeditor.core.storage.MediaStoreSaver
import com.videoeditor.core.storage.ScopedTempDir
import com.videoeditor.feature.compress.model.CompressionSettings
import com.videoeditor.feature.compress.model.EncodeProgress
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

class CompressWorkLauncher @Inject constructor(
    @ApplicationContext private val ctx: Context,
    private val ffmpegRunner: FFmpegRunner,
    private val videoProbe: VideoProbe,
    private val mediaStoreSaver: MediaStoreSaver,
    private val scopedTempDir: ScopedTempDir,
) {
    private var currentJob: Job? = null

    fun launch(
        uri: android.net.Uri,
        settings: CompressionSettings,
        onProgress: (EncodeProgress) -> Unit,
        onComplete: (android.net.Uri, Long, Boolean) -> Unit,
        onError: (String) -> Unit,
    ) {
        currentJob?.cancel()
        currentJob = kotlinx.coroutines.GlobalScope.launch {
            coroutineScope {
                val cachedFile = scopedTempDir.copyToCache(uri)
                val workDir = scopedTempDir.createWorkDir()
                val outputFile = java.io.File(workDir, "output.mp4")

                val source = videoProbe.probe(uri)
                val result = withContext(Dispatchers.IO) {
                    try {
                        ffmpegRunner.execute(
                            source = source,
                            settings = settings,
                            inputPath = cachedFile.absolutePath,
                            outputPath = outputFile.absolutePath,
                            totalDurationMs = source.durationMs,
                            onProgress = { encodeProgress ->
                                onProgress(encodeProgress)
                            },
                        )
                    } catch (e: Exception) {
                        scopedTempDir.cleanup(workDir)
                        scopedTempDir.cleanup(cachedFile.parentFile!!)
                        onError(e.message ?: "Unknown error")
                        return@withContext null
                    }
                }

                if (result == null) return@coroutineScope

                when (result) {
                    is RunResult.Success -> {
                        val outputSize = outputFile.length()
                        val savedUri = mediaStoreSaver.saveToGallery(outputFile, source.displayName)
                        scopedTempDir.cleanup(workDir)
                        scopedTempDir.cleanup(cachedFile.parentFile!!)
                        onProgress(EncodeProgress(percent = 1f))
                        onComplete(savedUri, outputSize, result.usedHardwareFallback)
                    }
                    is RunResult.Cancelled -> {
                        scopedTempDir.cleanup(workDir)
                        scopedTempDir.cleanup(cachedFile.parentFile!!)
                        onError("Compression cancelled")
                    }
                    is RunResult.Failed -> {
                        scopedTempDir.cleanup(workDir)
                        scopedTempDir.cleanup(cachedFile.parentFile!!)
                        onError(result.reason)
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