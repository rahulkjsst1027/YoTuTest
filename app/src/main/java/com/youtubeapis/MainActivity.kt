package com.youtubeapis

import android.os.Bundle
import android.util.Log
import android.view.View
import android.webkit.WebView
import android.widget.Button
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView

class MainActivity : AppCompatActivity() {

    var name = "Darshan- दर्शन"
    var name2 = "Technicalzest"
    var apiKey = "AIzaSyAa-NauHfEbtzg2YoyaUDz5Hq2yeIoHJCA"
    var player: YouTubePlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_FULLSCREEN or
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                )

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        val webView = findViewById<WebView>(R.id.webview)
      /*  val youTubePlayerView = findViewById<YouTubePlayerView>(R.id.youtube_player_view)
        lifecycle.addObserver(youTubePlayerView)

        youTubePlayerView.addYouTubePlayerListener(object : AbstractYouTubePlayerListener() {
            override fun onReady(youTubePlayer: YouTubePlayer) {
                player = youTubePlayer
            }
        })*/
        findViewById<Button>(R.id.btn).setOnClickListener {
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
    }


}
