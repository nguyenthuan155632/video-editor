package com.videoeditor.feature.compress.work

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat

object CompressForegroundInfo {
    const val CHANNEL_ID = "compress_encode"
    const val NOTIF_ID = 1001

    fun createNotification(context: Context, progress: Float): android.app.Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Video Compression",
                NotificationManager.IMPORTANCE_LOW,
            )
            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("Compressing video")
            .setContentText("${(progress * 100).toInt()}% complete")
            .setSmallIcon(android.R.drawable.ic_menu_save)
            .setOngoing(true)
            .setProgress(100, (progress * 100).toInt(), false)
            .build()
    }
}