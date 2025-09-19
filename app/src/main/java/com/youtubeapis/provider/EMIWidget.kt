package com.youtubeapis.provider

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.youtubeapis.R
import com.youtubeapis.receiver.WidgetUpdateReceiver
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class EMIWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (id in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, id)
        }
    }
    override fun onEnabled(context: Context) {
        val intent = Intent(context, WidgetUpdateReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.setRepeating(
            AlarmManager.RTC,
            System.currentTimeMillis(),
            60000L, // every 1 minute
            pendingIntent
        )
    }



    companion object {
        fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val views = RemoteViews(context.packageName, R.layout.widget_emi)

            views.setTextViewText(R.id.emiText, "Next EMI: ₹1,500")

            // 🕒 Show current time
            val time = SimpleDateFormat("hh:mm:ss", Locale.getDefault()).format(Date())
            views.setTextViewText(R.id.timeText, "Time: $time")

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
        /*fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
            val minWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)

            val layoutId = if (minWidth > 200) R.layout.widget_emi_large else R.layout.widget_emi
            val views = RemoteViews(context.packageName, layoutId)

            views.setTextViewText(R.id.emiText, "Next EMI: ₹1,500")
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }*/

    }
}

