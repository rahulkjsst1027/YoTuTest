package com.youtubeapis.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.youtubeapis.MyApp
import com.youtubeapis.dio.AppDatabase
import com.youtubeapis.utils.NotificationUtils.createForegroundNotification
import com.youtubeapis.worker.DownloadManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel

class DownloadService : Service() {
    private lateinit var db: AppDatabase
    private lateinit var scope: CoroutineScope
    private lateinit var downloadManager: DownloadManager

    override fun onCreate() {
        super.onCreate()
      //  db = Room.databaseBuilder(applicationContext, AppDatabase::class.java, "km-download-db").build()
       // scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val app = applicationContext as MyApp
        db = app.database
        scope = app.appScope
        downloadManager = DownloadManager.getInstance(applicationContext, db, scope)
        downloadManager.setOnAllDownloadsFinishedCallback {
            stopSelf()
        }
    }



    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: "ACTION_START"



        if (!::downloadManager.isInitialized) return START_NOT_STICKY

        when (action) {
            "ACTION_START" -> {
                val url = intent?.getStringExtra("url") ?: return START_NOT_STICKY
                val fileName = intent.getStringExtra("fileName") ?: "Downloading..."
                val ext = intent.getStringExtra("ext") ?: ""
                val mime = intent.getStringExtra("mime") ?: ""
                startForeground(100, createForegroundNotification(this, fileName))
                downloadManager.initialDownload(url, fileName, ext, mime)
            }

            "ACTION_RESUME", "ACTION_PAUSE", "ACTION_DELETE" -> {
                val downloadId = intent?.getLongExtra("id", -1L)
                if (downloadId != -1L) {
                    when (action) {
                        "ACTION_RESUME" -> {
                            downloadManager.resumeDownload(downloadId!!)
                        }
                        "ACTION_PAUSE" -> {
                            downloadManager.pauseDownload(downloadId!!)
                        }
                        "ACTION_DELETE" -> downloadManager.deleteDownload(downloadId!!)
                    }
                } else {
                    Log.e("DownloadService", "Missing download ID for $action")
                }
            }

            else -> Log.w("DownloadService", "Unknown action: $action")
        }

        return START_STICKY
    }



    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }



    override fun onBind(intent: Intent?): IBinder? = null
}
