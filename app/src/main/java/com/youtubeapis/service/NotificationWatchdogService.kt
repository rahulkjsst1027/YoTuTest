package com.youtubeapis.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.youtubeapis.R
import com.youtubeapis.provider.EMIWidget

class NotificationWatchdogService : Service() {

    private val watchdogHandler = Handler(Looper.getMainLooper())
    private val watchdogRunnable = object : Runnable {
        override fun run() {
           /* if (!isNotificationListenerServiceRunning(applicationContext)) {
                Log.w("Watchdog", "❌ NotificationListener not active! Rebinding now...")
                forceRebindNotificationService(applicationContext)
            } else {
                Log.d("Watchdog", "✅ NotificationListener is already active")
            }*/

            val manager = AppWidgetManager.getInstance(applicationContext)
            val ids = manager.getAppWidgetIds(ComponentName(applicationContext, EMIWidget::class.java))
            for (id in ids) {
                EMIWidget.updateAppWidget(applicationContext, manager, id)
            }


            // Re-run after 15 seconds
            watchdogHandler.postDelayed(this, 1000)
        }
    }


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(101, createNotification())
        watchdogHandler.post(watchdogRunnable)
        return START_STICKY
    }

    private fun createNotification(): Notification {
        val channelId = "watchdog_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Watchdog Notification",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Service Running")
            .setContentText("Ensuring notification listener is alive")
            .setSmallIcon(R.drawable.marketing)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setSilent(true)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        //watchdogHandler.removeCallbacks(watchdogRunnable)
        ContextCompat.startForegroundService(applicationContext, Intent(this, NotificationWatchdogService::class.java))

    }

    override fun onDestroy() {
        super.onDestroy()
        watchdogHandler.removeCallbacks(watchdogRunnable)
    }

}
