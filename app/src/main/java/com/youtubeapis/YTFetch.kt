package com.youtubeapis

import android.annotation.SuppressLint
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object YTFetch {
    fun fetchLatestVideo(query: String, apiKey: String, onResult: (Item?) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val channelResponse =
                    RetrofitClient.api.searchChannelByName(query = query, apiKey = apiKey)
                val channelId = channelResponse.items.firstOrNull()?.id?.channelId

                if (channelId != null) {
                    val liveResponse = RetrofitClient.api.searchVideosByChannel(
                        channelId = channelId,
                        eventType = "live",
                        apiKey = apiKey
                    )
                    val video = liveResponse.items.firstOrNull()

                    if (video != null /*&& video.snippet.liveBroadcastContent == "live"*/) {
                        withContext(Dispatchers.Main) { onResult(video) }
                        return@launch
                    }

                    val completedResponse = RetrofitClient.api.searchVideosByChannel(
                        channelId = channelId,
                        eventType = "completed",
                        apiKey = apiKey
                    )
                    val completedVideo = completedResponse.items.firstOrNull()

                    if (completedVideo != null) {
                        withContext(Dispatchers.Main) { onResult(completedVideo) }
                        return@launch
                    }

                    val latestResponse = RetrofitClient.api.searchVideosByChannel(
                        channelId = channelId,
                        eventType = null,
                        apiKey = apiKey
                    )
                    val latestVideo = latestResponse.items.firstOrNull()

                    withContext(Dispatchers.Main) { onResult(latestVideo) }
                } else {
                    withContext(Dispatchers.Main) { onResult(null) }
                }

            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) { onResult(null) }
            }
        }

    }

    @SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility")
    fun loadYoutubeVideoInWebView(webView: WebView, videoId: String) {
        val webSettings: WebSettings = webView.settings
        webSettings.javaScriptEnabled = true
        webSettings.domStorageEnabled = true
        webSettings.mediaPlaybackRequiresUserGesture = false
        webSettings.loadWithOverviewMode = true
        webSettings.useWideViewPort = true
        webSettings.builtInZoomControls = true
        webSettings.displayZoomControls = false

        webView.webChromeClient = WebChromeClient()
        webView.webViewClient = WebViewClient()

        // Disable user interaction
        webView.setOnTouchListener { _, _ -> true }

        val html = """
    <html>
        <body style="margin:0;padding:0;">
            <iframe width="100%" height="100%" 
                src="https://www.youtube.com/embed/$videoId?autoplay=1&modestbranding=0&rel=0&controls=0&showinfo=0&fs=0&cc_load_policy=1&showinfo=0&loading="lazy""
                frameborder="0" allowfullscreen>
            </iframe>
        </body>
    </html>
""".trimIndent()
        webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
    }
}
