package com.videoeditor.core.storage

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScopedTempDir @Inject constructor(@ApplicationContext private val ctx: Context) {

    private val baseDir: File
        get() = File(ctx.cacheDir, "compress").also { it.mkdirs() }

    fun createWorkDir(): File = File(baseDir, UUID.randomUUID().toString()).also { it.mkdirs() }

    fun copyToCache(uri: Uri): File {
        val workDir = createWorkDir()
        val dest = File(workDir, "input.mp4")
        ctx.contentResolver.openInputStream(uri)?.use { inp ->
            FileOutputStream(dest).use { out -> inp.copyTo(out) }
        }
        return dest
    }

    fun cleanup(workDir: File) {
        workDir.deleteRecursively()
    }

    fun cleanupAll() {
        baseDir.deleteRecursively()
    }
}