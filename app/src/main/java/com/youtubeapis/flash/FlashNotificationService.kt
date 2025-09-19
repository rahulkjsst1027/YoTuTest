package com.youtubeapis.flash

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

class FlashNotificationService : NotificationListenerService() {

    private lateinit var flashlight: FlashlightHelper
    private var blinking = false
    private var thread: Thread? = null

    override fun onCreate() {
        super.onCreate()
        flashlight = FlashlightHelper(this)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn != null) {
            startBlink()
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        stopBlink()
    }

    private fun startBlink() {
        if (blinking) return
        blinking = true
        thread = Thread {
            repeat(5) { // 5 baar blink hoga
                flashlight.setFlash(true)
                Thread.sleep(150)
                flashlight.setFlash(false)
                Thread.sleep(150)
            }
            blinking = false
        }
        thread?.start()
    }

    private fun stopBlink() {
        blinking = false
        flashlight.setFlash(false)
    }
}
