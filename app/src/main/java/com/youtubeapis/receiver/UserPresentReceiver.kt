package com.youtubeapis.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.youtubeapis.Utils.forceRebindNotificationService
import com.youtubeapis.service.NotificationWatchdogService

class UserPresentReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            "android.intent.action.QUICKBOOT_POWERON"
            //Intent.ACTION_USER_PRESENT
                -> {
                context.startService(Intent(context, NotificationWatchdogService::class.java))
                forceRebindNotificationService(context)
            }
            else -> {
                Log.w("Receiver", "❌ Unknown action: ${intent.action}")
            }
        }
    }


}
