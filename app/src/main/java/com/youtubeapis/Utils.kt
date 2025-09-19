package com.youtubeapis

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.provider.Settings
import com.youtubeapis.service.MyNotificationListenerService

object Utils {
    fun forceRebindNotificationService(context: Context) {
        val pm = context.packageManager
        val component = ComponentName(context, MyNotificationListenerService::class.java)
        pm.setComponentEnabledSetting(component, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP)
        pm.setComponentEnabledSetting(component, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP)
    }

    fun isNotificationListenerServiceRunning(context: Context): Boolean {
        val cn = ComponentName(context, MyNotificationListenerService::class.java)
        val enabledListeners = Settings.Secure.getString(
            context.contentResolver,
            "enabled_notification_listeners"
        )
        return enabledListeners?.contains(cn.flattenToString()) == true
    }

}