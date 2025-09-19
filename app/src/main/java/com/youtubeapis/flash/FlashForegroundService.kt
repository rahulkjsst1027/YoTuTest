package com.youtubeapis.flash

import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.youtubeapis.R

class FlashForegroundService : Service() {
    private lateinit var flashlightHelper: FlashlightHelper
    private lateinit var callFlashHelper: CallFlashHelper

  /*  val intent = Intent(this, FlashForegroundService::class.java)
    ContextCompat.startForegroundService(this, intent)*/


    override fun onCreate() {
        super.onCreate()
        flashlightHelper = FlashlightHelper(this)
        callFlashHelper = CallFlashHelper(this, flashlightHelper)

        // Call listening background me
        callFlashHelper.register()

        // Foreground notification (mandatory Android 8+)
        val notification = NotificationCompat.Builder(this, "flash_channel")
            .setContentTitle("Flash Service Running")
            .setContentText("Calls aur notifications ke liye flash enable hai")
            .setSmallIcon(R.drawable.ic_flash_on_yellow)
            .build()
        startForeground(1, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        callFlashHelper.unregister()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
