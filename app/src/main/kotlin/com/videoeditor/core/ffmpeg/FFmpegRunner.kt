package com.videoeditor.core.ffmpeg

import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.FFmpegSession
import com.arthenica.ffmpegkit.FFmpegSessionCompleteCallback
import com.arthenica.ffmpegkit.ReturnCode
import com.arthenica.ffmpegkit.Statistics
import com.arthenica.ffmpegkit.StatisticsCallback
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

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

    private var progressCallback: ((com.videoeditor.feature.compress.model.EncodeProgress) -> Unit)? = null

    fun setProgressCallback(cb: (com.videoeditor.feature.compress.model.EncodeProgress) -> Unit) {
        progressCallback = cb
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
            // This fork uses executeWithArguments (String[]) not execute(String)
            val session = FFmpegKit.executeWithArguments(args)
            lastSessionId = session.sessionId

            val rc = session.returnCode
            val allStats = session.allStatistics
            if (allStats.isNotEmpty()) {
                val last = allStats.last()
                val prog = progressParser.parse(
                    "out_time_us=${(last.time * 1000).toLong()}\nframe=${last.videoFrameNumber}\nfps=${last.videoFps}",
                    totalDurationMs,
                )
                if (prog != null) progressCallback?.invoke(prog)
            }

            return when {
                ReturnCode.isSuccess(rc) -> RunResult.Success(false)
                ReturnCode.isCancel(rc) -> RunResult.Cancelled(false)
                else -> RunResult.Failed(session.failStackTrace ?: "Encoding failed", false)
            }
        }

        // Try hardware first
        var result = run(true)
        if (result is RunResult.Failed && settings.useHardwareAccel) {
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