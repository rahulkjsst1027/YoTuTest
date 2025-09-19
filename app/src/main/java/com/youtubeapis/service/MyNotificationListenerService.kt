package com.youtubeapis.service

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log

class MyNotificationListenerService : NotificationListenerService() {
    override fun onListenerConnected() {
        Log.d("NOTI_LISTENER", "✅ Listener connected")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        Log.d("packageName", "📢 dsadasasfas ${sbn?.notification?.extras?.getString(Notification.EXTRA_TITLE)}")
        Log.d("packageName", "📢 dsadasasfas ${sbn?.notification?.extras?.getString(Notification.EXTRA_TEXT)}")
        val packageName = sbn?.packageName ?: return

        if (packageName == "com.google.android.youtube") {
            val extras = sbn.notification.extras
            val title = extras.getString(Notification.EXTRA_TITLE)
            val text = extras.getString(Notification.EXTRA_TEXT)

            Log.d("YT_NOTIFY", "📢 Title: $title, Text: $text")

            // Example: Forward to server, show custom UI, save locally, etc.
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        Log.d("packageName", "📢 rr  dsadasasfas ${sbn?.notification}")
    }
}
