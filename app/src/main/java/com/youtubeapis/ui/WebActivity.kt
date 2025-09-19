package com.youtubeapis.ui

import android.app.DownloadManager
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.MimeTypeMap
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.youtubeapis.MyApp
import com.youtubeapis.R
import com.youtubeapis.databinding.ActivityWebBinding
import com.youtubeapis.dio.AppDatabase
import com.youtubeapis.service.DownloadService
import kotlinx.coroutines.CoroutineScope
import org.json.JSONArray
import java.net.URLDecoder


class WebActivity : AppCompatActivity() {
    private lateinit var db: AppDatabase
    private lateinit var scope: CoroutineScope
    lateinit var binding: ActivityWebBinding
    private val recentDownloads = mutableSetOf<String>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityWebBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val app = applicationContext as MyApp
        db = app.database
        scope = app.appScope

        loadWebView()


        binding.go.setOnClickListener {
            startActivity(Intent(this, DownlodActivity::class.java))
        }
    }

    private lateinit var handler: Handler
    private lateinit var runnable: Runnable

    private fun loadWebView() {
        binding.webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = true
            allowContentAccess = true
            setSupportMultipleWindows(false)
            loadsImagesAutomatically = true
            mediaPlaybackRequiresUserGesture = false
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            binding.webView.settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
        }

        binding.webView.setDownloadListener { url, userAgent, contentDisposition, mimeType, contentLength ->

            if (recentDownloads.contains(url)) {
                Log.d("WebDownload", "Skipping duplicate: $url")
                return@setDownloadListener
            }
            val fileName = getFileNameFromUrl(url, mimeType)
            //  val fileName = URLUtil.guessFileName(url, contentDisposition, mimeType)
            val guessedExt = MimeTypeMap.getFileExtensionFromUrl(url).lowercase()
            val guessedMime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(guessedExt)
            val mt = guessedMime ?: "application/octet-stream"


            recentDownloads.add(url)
            Log.d("WebDownload", "Detected: $url, File: $fileName, mimeType $mimeType")
            Log.d("WebDownload", "Detected: ext $guessedExt, mimeType $mt contentDisposition $contentDisposition")

            val intent = Intent(this@WebActivity, DownloadService::class.java).apply {
                putExtra("url", url)
                putExtra("fileName", fileName)
                putExtra("ext", guessedExt)
                putExtra("mime", mt)
            }
            startService(intent)

            Toast.makeText(this, "Downloading: $fileName", Toast.LENGTH_SHORT).show()
        }

        // Bind JS interface
        binding.webView.addJavascriptInterface(JSBridge(), "AndroidBridge")
        binding.webView.setWebViewClient(object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
                // remove any overlays from previous page
                removeOverlayButtons()

                // inject the persistent detector script (it self-de-duplicates)
                injectPersistentVideoDetector()
            }
        })



        val initialUrl = intent.getStringExtra("web_url") ?: "https://www.google.com"

        binding.webView.loadUrl(initialUrl)
    }
    // --------- inject script that will push events to AndroidBridge ----------
    private fun injectPersistentVideoDetector() {
        val script = """
(function() {
  if (window.__android_video_detector_installed) return;
  window.__android_video_detector_installed = true;

  function safeCallBridgeVideo(url) {
    try {
      if (window.AndroidBridge && typeof window.AndroidBridge.onVideoFound === 'function') {
        window.AndroidBridge.onVideoFound(url);
      }
    } catch(e) { console && console.log && console.log('bridge video call failed', e); }
  }

  function safeCallBridgeIframeList(urls) {
    try {
      if (window.AndroidBridge && typeof window.AndroidBridge.onIframeList === 'function') {
        window.AndroidBridge.onIframeList(JSON.stringify(urls));
      }
    } catch(e) { console && console.log && console.log('bridge iframe list call failed', e); }
  }

  function createButtonForVideo(video) {
    try {
      if (!video || video.dataset.__hasDownloadBtn) return;
      video.dataset.__hasDownloadBtn = '1';

      var parent = video.parentElement;
      if (parent && window.getComputedStyle(parent).position === 'static') {
        parent.style.position = 'relative';
      }

      var btn = document.createElement('button');
      btn.className = 'android_download_btn_overlay';
      btn.textContent = '⬇️';
      btn.style.position = 'absolute';
      btn.style.top = '50%';
      btn.style.right = '12px';
      btn.style.transform = 'translateY(-50%)';
      btn.style.padding = '8px 10px';
      btn.style.fontSize = '13px';
      btn.style.borderRadius = '8px';
      btn.style.border = 'none';
      btn.style.background = 'rgba(229,57,53,0.95)';
      btn.style.color = '#fff';
      btn.style.zIndex = 9999999;
      btn.style.cursor = 'pointer';
      btn.style.boxShadow = '0 2px 6px rgba(0,0,0,0.35)';
      btn.style.display = 'none'; // hidden by default

      btn.onclick = function(e) {
        e.stopPropagation();
        e.preventDefault();
        var src = video.currentSrc || video.src || (video.querySelector && (video.querySelector('source')||{}).src) || '';
        if (src) {
          safeCallBridgeVideo(src);
        } else {
          safeCallBridgeVideo(window.location.href || '');
        }
      };

      parent.appendChild(btn);

      // events
      video.addEventListener('pause', function() {
        if (video.__inViewport) btn.style.display = 'block';
      });
      video.addEventListener('play', function() {
        btn.style.display = 'none';
      });

      // observe if video is visible in viewport
      var io = new IntersectionObserver(function(entries) {
        entries.forEach(function(entry) {
          if (entry.isIntersecting) {
            video.__inViewport = true;
            if (video.paused) btn.style.display = 'block';
          } else {
            video.__inViewport = false;
            btn.style.display = 'none';
          }
        });
      }, { threshold: 0.5 });
      io.observe(video);

    } catch (ex) { console && console.log && console.log('createButtonForVideo error', ex); }
  }

  function scanAndAttach() {
    try {
      var videos = document.querySelectorAll('video');
      videos.forEach(function(v){
        try { createButtonForVideo(v); } catch(e) {}
      });

      var iframes = document.querySelectorAll('iframe');
      var urls = [];
      iframes.forEach(function(f){
        try {
          var s = f.src || f.getAttribute('src') || '';
          if (!s) return;
          if (f.dataset.__iframeReported) return;
          f.dataset.__iframeReported = '1';
          urls.push(s);
        } catch(e) {}
      });
      if (urls.length) {
        safeCallBridgeIframeList(urls);
      }
    } catch(e) { console && console.log && console.log('scanAndAttach error', e); }
  }

  scanAndAttach();

  var mo = new MutationObserver(function(mutations){
    var changed = false;
    for (var i=0;i<mutations.length;i++) {
      var m = mutations[i];
      if (m.addedNodes && m.addedNodes.length) { changed = true; break; }
      if (m.type === 'attributes' && (m.attributeName === 'src' || m.attributeName === 'class')) { changed = true; break; }
    }
    if (changed) {
      setTimeout(scanAndAttach, 200);
    }
  });

  try {
    mo.observe(document.documentElement || document.body, { childList: true, subtree: true, attributes: true, attributeFilter: ['src','class'] });
  } catch(e) {}

  var tries = 0;
  var periodic = setInterval(function(){
    scanAndAttach();
    tries++;
    if (tries > 10) clearInterval(periodic);
  }, 3000);

})();
""".trimIndent()



        // inject (no need to parse result)
        runOnUiThread {
            try {
                binding.webView.evaluateJavascript(script, null)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // ---------- show android overlay button (for iframe or fallback) ----------
    private fun showAndroidOverlayButton(url: String, tagSuffix: String = "") {
        runOnUiThread {
            try {
                // remove any existing overlay with same tagSuffix (optional)
                removeOverlayButtons()

                val overlayBtn = Button(this).apply {
                    text = "⬇️ Download"
                    setBackgroundColor(Color.parseColor("#E53935"))
                    setTextColor(Color.WHITE)
                    setPadding(24, 12, 24, 12)
                    textSize = 14f
                    // tag so we can remove later
                    tag = "overlay_btn$tagSuffix"
                    // rounded background on older devices (optional)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        elevation = 8f
                    }
                    setOnClickListener {
                        Log.d("WEBVIEW", "overlay click -> $url")
                        // handle download or open external player
                        // example: open in browser
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                            startActivity(intent)
                        } catch (ex: Exception) {
                            Toast.makeText(this@WebActivity, "Cannot open $url", Toast.LENGTH_SHORT).show()
                        }
                    }
                }

                val params = RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.WRAP_CONTENT,
                    RelativeLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    addRule(RelativeLayout.ALIGN_PARENT_BOTTOM)
                    addRule(RelativeLayout.ALIGN_PARENT_END)
                    setMargins(0,0,40,120) // right bottom offset
                }

                val root = findViewById<ViewGroup>(R.id.main)
                root.addView(overlayBtn, params)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun removeOverlayButtons() {
        runOnUiThread {
            try {
                val root = findViewById<ViewGroup>(R.id.main)
                val toRemove = mutableListOf<View>()
                for (i in 0 until root.childCount) {
                    val c = root.getChildAt(i)
                    val t = c.tag
                    if (t != null && (t as? String)?.startsWith("overlay_btn") == true) {
                        toRemove.add(c)
                    }
                }
                toRemove.forEach { root.removeView(it) }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // ------------------ JS Bridge -----------------------
    inner class JSBridge {
        @JavascriptInterface
        fun onVideoFound(videoUrl: String) {
            Log.d("JSBridge", "onVideoFound -> $videoUrl")
            // You can start download here or show Android-level UI
            // Example: just show overlay button as fallback or downloader
            // If the videoUrl is same-origin/video src, you might start DownloadManager directly
            showAndroidOverlayButton(videoUrl, "_video")
        }

       /* @JavascriptInterface
        fun onIframeFound(iframeUrl: String) {
            Log.d("JSBridge", "onIframeFound -> $iframeUrl")
            // For cross-origin iframe (like YouTube) we can't inject inside -> show Android overlay
            showAndroidOverlayButton(iframeUrl, "_iframe")
        }*/

        @JavascriptInterface
        fun onIframeList(jsonArray: String) {
            val arr = JSONArray(jsonArray)
            for (i in 0 until arr.length()) {
                val url = arr.getString(i)
                Log.d("JSBridge", "Iframe[$i] -> $url")
                showAndroidOverlayButton(url, "_iframe")
            }
        }

    }

    private fun downloadVideo(url: String) {
        val request: DownloadManager.Request = DownloadManager.Request(Uri.parse(url))
        request.setTitle("Video Download")
        request.setDescription("Downloading...")
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        request.setDestinationInExternalPublicDir(
            Environment.DIRECTORY_DOWNLOADS,
            "downloaded_video.mp4"
        )

        val manager: DownloadManager = getSystemService(DOWNLOAD_SERVICE) as DownloadManager
        if (manager != null) {
            manager.enqueue(request)
        }
    }

    fun getFileNameFromUrl(url: String, mimeType: String?): String {
        try {
            val decodedUrl = URLDecoder.decode(url, "UTF-8")
            var fileName = decodedUrl.substringAfterLast("/")
                .substringBefore("?")
                .substringBefore("#")

            // If fileName doesn't contain a proper extension, add one using MIME type
            if (!fileName.contains(".") && !mimeType.isNullOrBlank()) {
                val extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType)
                if (!extension.isNullOrBlank()) {
                    fileName += ".$extension"
                }
            }

            return fileName
        } catch (e: Exception) {
            return "unknown_file_${System.currentTimeMillis()}"
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        binding.webView.destroy()
        if (::handler.isInitialized) handler.removeCallbacks(runnable)
    }


    override fun onBackPressed() {
        if (binding.webView.canGoBack()) {
            binding.webView.goBack()
        } else {
            super.onBackPressed()
        }
    }


}