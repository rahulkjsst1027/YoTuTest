package com.youtubeapis.utils

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat


object NotificationUtils {


    fun createForegroundNotification(context: Context, title: String): Notification {
        val builder = NotificationCompat.Builder(context, "download_channel")
            .setContentTitle("Downloading: $title")
            .setContentText("Running in background...")
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setPriority(NotificationCompat.PRIORITY_LOW)

        return builder.build()
    }

    fun showDownloadNotification(
        id : Int,
        context: Context,
        fileName: String,
        progress: Int,
        speed: Long,
        remaining: Long
    ) {
        val contentText = "${Helper.formatSpeed(speed)} • ${Helper.formatTimeRemaining(remaining)}"

        val builder = NotificationCompat.Builder(context, "download_channel")
            .setContentTitle("Downloading...: $fileName")
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setProgress(100, progress, false)
            .setOnlyAlertOnce(true)
            .setOngoing(progress < 100)

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(id, builder.build())
    }

}

