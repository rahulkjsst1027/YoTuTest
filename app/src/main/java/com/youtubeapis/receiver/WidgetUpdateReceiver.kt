package com.youtubeapis.receiver

import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import com.youtubeapis.provider.EMIWidget

class WidgetUpdateReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val manager = AppWidgetManager.getInstance(context)
        val ids = manager.getAppWidgetIds(ComponentName(context, EMIWidget::class.java))
        for (id in ids) {
            EMIWidget.updateAppWidget(context, manager, id)
        }
    }
}
