package com.videoeditor.core.ffmpeg

import android.util.Log
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.LogCallback
import com.arthenica.ffmpegkit.ReturnCode
import com.arthenica.ffmpegkit.StatisticsCallback
import com.videoeditor.feature.compress.model.EncodeProgress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

private const val TAG = "FFmpegRunner"

sealed class RunResult {
    data class Success(val usedHardwareFallback: Boolean = false) : RunResult()
    data class Cancelled(val usedHardwareFallback: Boolean = false) : RunResult()
    data class Failed(val reason: String, val usedHardwareFallback: Boolean = false) : RunResult()
}


@Singleton
class FFmpegRunner @Inject constructor(
    private val commandBuilder: FFmpegCommandBuilder,
) {
    private var lastSessionId: Long? = null
    private var lastUsedHwFallback = false

    suspend fun execute(
        source: com.videoeditor.core.probe.ProbeResult,
        settings: com.videoeditor.feature.compress.model.CompressionSettings,
        inputPath: String,
        outputPath: String,
        totalDurationMs: Long,
        onProgress: (EncodeProgress) -> Unit = {},
    ): RunResult = withContext(Dispatchers.IO) {
        lastUsedHwFallback = false

        suspend fun run(hw: Boolean): RunResult = suspendCancellableCoroutine { cont ->
            val args = commandBuilder.build(source, settings, inputPath, outputPath, hw)
            val command = args.joinToString(" ")

            Log.d(TAG, "Running: ffmpeg $command")

            val logCallback = LogCallback { log ->
                Log.v(TAG, log.message ?: "")
            }

            val statisticsCallback = StatisticsCallback { stats ->
                if (totalDurationMs > 0) {
                    val percent = (stats.time.toFloat() / totalDurationMs).coerceIn(0f, 1f)
                    onProgress(
                        EncodeProgress(
                            percent = percent,
                            frame = stats.videoFrameNumber.toLong(),
                            fps = stats.videoFps.toDouble(),
                        )
                    )
                }
            }

            val session = FFmpegKit.executeAsync(
                command,
                { session ->
                    val rc = session.returnCode
                    val output = session.output
                    val failStack = session.failStackTrace

                    Log.d(TAG, "FFmpeg exit code: ${rc?.value}, output: $output, failStack: $failStack")

                    val result: RunResult = when {
                        ReturnCode.isSuccess(rc) -> RunResult.Success(false)
                        ReturnCode.isCancel(rc) -> RunResult.Cancelled(false)
                        else -> {
                            val reason = buildString {
                                append(failStack ?: "Unknown error")
                                if (!output.isNullOrBlank()) {
                                    append(". FFmpeg output: ")
                                    append(output.takeLast(500))
                                }
                            }
                            RunResult.Failed(reason.take(500), false)
                        }
                    }
                    cont.resume(result)
                },
                logCallback,
                statisticsCallback,
            )

            lastSessionId = session.sessionId
            lastUsedHwFallback = hw
        }

        // Try hardware first, fall back to software
        var result = run(true)
        if (result is RunResult.Failed && settings.useHardwareAccel) {
            Log.d(TAG, "Hardware encode failed, falling back to software: ${result.reason}")
            lastSessionId?.let { FFmpegKit.cancel(it) }
            lastUsedHwFallback = true
            result = run(false)
        }

        when (result) {
            is RunResult.Success -> result.copy(usedHardwareFallback = lastUsedHwFallback)
            is RunResult.Cancelled -> result.copy(usedHardwareFallback = lastUsedHwFallback)
            is RunResult.Failed -> result.copy(usedHardwareFallback = lastUsedHwFallback)
        }
    }

    fun cancel() {
        lastSessionId?.let { FFmpegKit.cancel(it) }
        lastSessionId = null
    }
}