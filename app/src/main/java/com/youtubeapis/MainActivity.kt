package com.youtubeapis

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.webkit.WebView
import android.widget.Button
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.youtubeapis.service.MyNotificationListenerService
import com.youtubeapis.service.NotificationWatchdogService

class MainActivity : AppCompatActivity() {

    var name = "Darshan- दर्शन"
    var name2 = "Technicalzest"
    var apiKey = "AIzaSyAa-NauHfEbtzg2YoyaUDz5Hq2yeIoHJCA"
    var player: YouTubePlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        ContextCompat.startForegroundService(this, Intent(this, NotificationWatchdogService::class.java))


        val webView = findViewById<WebView>(R.id.webview)
      /*  val youTubePlayerView = findViewById<YouTubePlayerView>(R.id.youtube_player_view)
        lifecycle.addObserver(youTubePlayerView)

        youTubePlayerView.addYouTubePlayerListener(object : AbstractYouTubePlayerListener() {
            override fun onReady(youTubePlayer: YouTubePlayer) {
                player = youTubePlayer
            }
        })*/
        findViewById<Button>(R.id.btn).setOnClickListener {
            requestNotificationAccess(this)
            return@setOnClickListener


            LoadingDialogFragment.show(supportFragmentManager) // Show loader
            YTFetch.fetchLatestVideo(name, apiKey) { video ->
                LoadingDialogFragment.dismiss() // Dismiss loader

                if (video != null) {
                    val videoId = video.id.videoId
                    val url = "https://www.youtube.com/watch?v=$videoId"
                    Toast.makeText(application, url, Toast.LENGTH_SHORT).show()
                    Log.d("YouTube", "Video URL: $url")
                   // videoId?.let { it1 -> player?.cueVideo(it1, 0f) }
                    videoId?.let { it1 -> YTFetch.loadYoutubeVideoInWebView(webView, it1) }

                } else {
                    Log.d("YouTube", "No video found.")
                }
            }
        }

        findViewById<Button>(R.id.hit).setOnClickListener {
            forceRebindNotificationService(this)
            sendTestNotification()
        }

        findViewById<Button>(R.id.opti).setOnClickListener {
            Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:$packageName")
                startActivity(this)
            }

        }

        // 🔔 Create Notification Channel (required for Android 8+)
        createTestNotificationChannel()

        // 🔥 Send a test notification
       // sendTestNotification()



    }



    private fun sendTestNotification() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val notification = NotificationCompat.Builder(this, "test_channel")
            .setContentTitle("MrBeast")
            .setContentText("Uploaded a new video!")
            .setSmallIcon(R.mipmap.ic_launcher) // use any valid icon from your res/drawable
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        notificationManager.notify(999, notification)
    }

    private fun createTestNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "test_channel",
                "Test Channel",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Channel for testing YouTube-style notifications"
            }

            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }


    fun requestNotificationAccess(context: Context) {
        try {
            val intent = Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Unable to open settings", Toast.LENGTH_SHORT).show()
        }

    }

    fun forceRebindNotificationService(context: Context) {
        val pm = context.packageManager
        val component = ComponentName(context, MyNotificationListenerService::class.java)
        pm.setComponentEnabledSetting(component, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP)
        pm.setComponentEnabledSetting(component, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP)
    }



}
