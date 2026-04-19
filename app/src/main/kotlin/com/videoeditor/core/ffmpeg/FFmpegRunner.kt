package com.videoeditor.core.ffmpeg

import android.util.Log
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "FFmpegRunner"

sealed class RunResult {
    data class Success(val usedHardwareFallback: Boolean = false) : RunResult()
    data class Cancelled(val usedHardwareFallback: Boolean = false) : RunResult()
    data class Failed(val reason: String, val usedHardwareFallback: Boolean = false) : RunResult()
}

data class ProgressUpdate(
    val progress: com.videoeditor.feature.compress.model.EncodeProgress,
    val usedHardwareFallback: Boolean,
)

@Singleton
class FFmpegRunner @Inject constructor(
    private val commandBuilder: FFmpegCommandBuilder,
    private val progressParser: ProgressParser,
) {
    private var lastSessionId: Long? = null
    private var lastUsedHwFallback = false

    fun setProgressCallback(cb: (com.videoeditor.feature.compress.model.EncodeProgress) -> Unit) {
        // Reserved for future progress extraction from FFmpeg output
    }

    suspend fun execute(
        source: com.videoeditor.core.probe.ProbeResult,
        settings: com.videoeditor.feature.compress.model.CompressionSettings,
        inputPath: String,
        outputPath: String,
        totalDurationMs: Long,
    ): RunResult = withContext(Dispatchers.IO) {
        lastUsedHwFallback = false

        suspend fun run(hw: Boolean): RunResult {
            val args = commandBuilder.build(source, settings, inputPath, outputPath, hw)
            val command = args.joinToString(" ")

            Log.d(TAG, "Running: ffmpeg $command")

            val session = FFmpegKit.executeWithArguments(args)
            lastSessionId = session.sessionId

            val rc = session.returnCode
            val output = session.output
            val failStack = session.failStackTrace

            Log.d(TAG, "FFmpeg exit code: ${rc?.value}, output: $output, failStack: $failStack")

            return when {
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