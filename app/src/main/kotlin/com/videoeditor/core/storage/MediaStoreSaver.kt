package com.videoeditor.core.storage

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaStoreSaver @Inject constructor(@ApplicationContext private val ctx: Context) {

    suspend fun saveToGallery(sourceFile: File, displayName: String): Uri = withContext(Dispatchers.IO) {
        val mimeType = when {
            displayName.endsWith(".mp4", ignoreCase = true) -> "video/mp4"
            displayName.endsWith(".mkv", ignoreCase = true) -> "video/x-matroska"
            else -> "video/mp4"
        }

        val contentValues = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, displayName)
            put(MediaStore.Video.Media.MIME_TYPE, mimeType)
            put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/VideoEditor")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Video.Media.IS_PENDING, 1)
            }
        }

        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        }

        val uri = ctx.contentResolver.insert(collection, contentValues)
            ?: throw IllegalStateException("Failed to create MediaStore entry")

        ctx.contentResolver.openOutputStream(uri)?.use { out ->
            FileInputStream(sourceFile).use { inp ->
                inp.copyTo(out)
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            contentValues.clear()
            contentValues.put(MediaStore.Video.Media.IS_PENDING, 0)
            ctx.contentResolver.update(uri, contentValues, null, null)
        }

        uri
    }
}