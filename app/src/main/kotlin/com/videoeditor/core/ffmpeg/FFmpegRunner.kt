package com.videoeditor.core.ffmpeg

import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

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

    fun runEncode(
        source: com.videoeditor.core.probe.ProbeResult,
        settings: com.videoeditor.feature.compress.model.CompressionSettings,
        inputPath: String,
        outputPath: String,
        totalDurationMs: Long,
    ): Flow<ProgressUpdate> = flow {
        var usedHwFallback = false

        val executeSession: (Boolean) -> Boolean = { hw ->
            val args = commandBuilder.build(source, settings, inputPath, outputPath, hw)
            val command = args.joinToString(" ")
            val session = FFmpegKit.execute(command)
            lastSessionId = session.sessionId
            ReturnCode.isSuccess(session.returnCode)
        }

        var success = executeSession(true)
        if (!success && settings.useHardwareAccel) {
            lastSessionId?.let { FFmpegKit.cancel(it) }
            usedHwFallback = true
            success = executeSession(false)
        }

        if (success) {
            emit(ProgressUpdate(
                progress = com.videoeditor.feature.compress.model.EncodeProgress(percent = 1f),
                usedHardwareFallback = usedHwFallback,
            ))
        }
    }.flowOn(Dispatchers.IO)

    suspend fun execute(
        source: com.videoeditor.core.probe.ProbeResult,
        settings: com.videoeditor.feature.compress.model.CompressionSettings,
        inputPath: String,
        outputPath: String,
        totalDurationMs: Long,
    ): RunResult = withContext(Dispatchers.IO) {
        var usedHwFallback = false

        val doExec: (Boolean) -> Boolean = { hw ->
            val args = commandBuilder.build(source, settings, inputPath, outputPath, hw)
            val command = args.joinToString(" ")
            val session = FFmpegKit.execute(command)
            lastSessionId = session.sessionId
            ReturnCode.isSuccess(session.returnCode)
        }

        var success = doExec(true)
        if (!success && settings.useHardwareAccel) {
            lastSessionId?.let { FFmpegKit.cancel(it) }
            usedHwFallback = true
            success = doExec(false)
        }

        when {
            success -> RunResult.Success(usedHwFallback)
            else -> RunResult.Failed("Compression failed", usedHwFallback)
        }
    }

    fun cancel() {
        lastSessionId?.let { FFmpegKit.cancel(it) }
    }
}