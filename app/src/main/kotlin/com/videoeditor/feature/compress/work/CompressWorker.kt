package com.videoeditor.feature.compress.work

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.videoeditor.R
import com.videoeditor.core.estimator.OutputSizeEstimator
import com.videoeditor.core.ffmpeg.FFmpegRunner
import com.videoeditor.core.ffmpeg.RunResult
import com.videoeditor.core.probe.VideoProbe
import com.videoeditor.core.storage.MediaStoreSaver
import com.videoeditor.core.storage.ScopedTempDir
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.onEach
import java.io.File
import javax.inject.Inject

class CompressWorker(
    @ApplicationContext ctx: Context,
    params: WorkerParameters,
) : CoroutineWorker(ctx, params) {

    @Inject lateinit var ffmpegRunner: FFmpegRunner
    @Inject lateinit var videoProbe: VideoProbe
    @Inject lateinit var estimator: OutputSizeEstimator
    @Inject lateinit var mediaStoreSaver: MediaStoreSaver
    @Inject lateinit var scopedTempDir: ScopedTempDir

    companion object {
        const val KEY_SOURCE_URI = "source_uri"
        const val KEY_CODEC = "codec"
        const val KEY_RATE_CONTROL = "rate_control"
        const val KEY_CRF = "crf"
        const val KEY_TARGET_BITRATE = "target_bitrate"
        const val KEY_FPS = "fps"
        const val KEY_RESOLUTION = "resolution"
        const val KEY_PRESET = "preset"
        const val KEY_HW_ACCEL = "hw_accel"
        const val KEY_AUDIO_BITRATE = "audio_bitrate"
        const val KEY_AUDIO_CHANNELS = "audio_channels"
        const val KEY_WORK_DIR = "work_dir"

        const val CHANNEL_ID = "compress_encode"
        const val NOTIF_ID = 1001
    }

    override suspend fun doWork(): Result {
        val sourceUri = inputData.getString(KEY_SOURCE_URI)
            ?: return Result.failure()

        setForeground(createForegroundInfo(0f))

        val uri = android.net.Uri.parse(sourceUri)
        val cachedFile = scopedTempDir.copyToCache(uri)
        val workDir = scopedTempDir.createWorkDir()
        val outputFile = File(workDir, "output.mp4")

        val settings = buildCompressionSettings()
        val source = videoProbe.probe(uri)

        val progressFlow = ffmpegRunner.runEncode(
            source = source,
            settings = settings,
            inputPath = cachedFile.absolutePath,
            outputPath = outputFile.absolutePath,
            totalDurationMs = source.durationMs,
        )

        progressFlow.onEach { update ->
            setProgress(workDataOf("progress" to update.progress.percent))
        }.collect { runResult ->
            when (runResult) {
                is RunResult.Success -> {
                    val savedUri = mediaStoreSaver.saveToGallery(outputFile, source.displayName)
                    scopedTempDir.cleanup(workDir)
                    scopedTempDir.cleanup(cachedFile.parentFile)
                    Result.success(workDataOf("output_uri" to savedUri.toString()))
                }
                is RunResult.Cancelled -> {
                    scopedTempDir.cleanup(workDir)
                    Result.failure()
                }
                is RunResult.Failed -> {
                    scopedTempDir.cleanup(workDir)
                    Result.failure()
                }
            }
        }

        return Result.success()
    }

    private fun buildCompressionSettings() = com.videoeditor.feature.compress.model.CompressionSettings(
        codec = com.videoeditor.feature.compress.model.VideoCodec.valueOf(
            inputData.getString(KEY_CODEC) ?: "H264"
        ),
        rateControl = com.videoeditor.feature.compress.model.RateControl.valueOf(
            inputData.getString(KEY_RATE_CONTROL) ?: "CRF"
        ),
        crf = inputData.getInt(KEY_CRF, 23),
        targetBitrateKbps = inputData.getInt(KEY_TARGET_BITRATE, 2500),
        fps = com.videoeditor.feature.compress.model.FpsChoice.valueOf(
            inputData.getString(KEY_FPS) ?: "KEEP"
        ),
        resolution = inputData.getString(KEY_RESOLUTION)?.let {
            com.videoeditor.feature.compress.model.ResolutionPreset.valueOf(it)
        },
        preset = com.videoeditor.feature.compress.model.EncodingPreset.valueOf(
            inputData.getString(KEY_PRESET) ?: "MEDIUM"
        ),
        useHardwareAccel = inputData.getBoolean(KEY_HW_ACCEL, true),
        audio = com.videoeditor.feature.compress.model.AudioSettings(
            bitrateKbps = inputData.getInt(KEY_AUDIO_BITRATE, 128),
            channels = com.videoeditor.feature.compress.model.AudioChannels.valueOf(
                inputData.getString(KEY_AUDIO_CHANNELS) ?: "STEREO"
            ),
        ),
    )

    private fun createForegroundInfo(progress: Float): ForegroundInfo {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Video Compression",
                NotificationManager.IMPORTANCE_LOW,
            )
            val notificationManager = applicationContext.getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle("Compressing video")
            .setContentText("${(progress * 100).toInt()}% complete")
            .setSmallIcon(android.R.drawable.ic_menu_save)
            .setOngoing(true)
            .setProgress(100, (progress * 100).toInt(), false)
            .build()

        return ForegroundInfo(NOTIF_ID, notification)
    }
}