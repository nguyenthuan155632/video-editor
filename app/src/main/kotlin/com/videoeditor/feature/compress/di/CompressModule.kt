package com.videoeditor.feature.compress.di

import com.videoeditor.core.estimator.OutputSizeEstimator
import com.videoeditor.core.ffmpeg.FFmpegCommandBuilder
import com.videoeditor.core.ffmpeg.FFmpegRunner
import com.videoeditor.core.ffmpeg.ProgressParser
import com.videoeditor.core.probe.VideoProbe
import com.videoeditor.core.storage.MediaStoreSaver
import com.videoeditor.core.storage.ScopedTempDir
import com.videoeditor.feature.compress.work.CompressWorkLauncher
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object CompressModule {

    @Provides
    fun provideCompressWorkLauncher(
        @ApplicationContext ctx: android.content.Context,
        runner: FFmpegRunner,
        probe: VideoProbe,
        estimator: OutputSizeEstimator,
        saver: MediaStoreSaver,
        tempDir: ScopedTempDir,
    ): CompressWorkLauncher = CompressWorkLauncher(ctx, runner, probe, estimator, saver, tempDir)
}