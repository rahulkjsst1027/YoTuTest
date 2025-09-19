package com.youtubeapis.files

import android.annotation.SuppressLint
import android.app.PictureInPictureParams
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Color
import android.graphics.Matrix
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.util.Rational
import android.view.GestureDetector
import android.view.MenuItem
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.TextureView
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.PopupMenu
import androidx.activity.addCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.MediaSession
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.TrackSelectionDialogBuilder
import com.youtubeapis.R
import com.youtubeapis.databinding.ActivityVideoPlayerBinding
import com.youtubeapis.databinding.ExoPlayerCustomControllerBinding
import java.io.File
import kotlin.math.abs


@SuppressLint("UnsafeOptInUsageError")
class VideoPlayerActivity : AppCompatActivity() {

    // -------------------- View Binding -------------------- //
    private lateinit var binding: ActivityVideoPlayerBinding
    private lateinit var cBinding: ExoPlayerCustomControllerBinding

    // -------------------- Player & Media -------------------- //
    private var player: ExoPlayer? = null
    private var mediaSession: MediaSession? = null
    private var uri: Uri? = null

    // -------------------- Playback State -------------------- //
    private var playWhenReady = true
    private var currentWindow = 0
    private var playbackPosition = 0L
    private var isFullscreen = false
    private var isLock = false
    private var isZoomed = false
    private var isLongPress = false

    // -------------------- Gesture Handling -------------------- //
    private lateinit var gestureDetector: GestureDetector
    private lateinit var scaleGestureDetector: ScaleGestureDetector
    private var isMultiTouch = false
    private var activePointerId = MotionEvent.INVALID_POINTER_ID

    // Gesture state
    private var startX = 0f
    private var startY = 0f
    private var isSwiping = false
    private var swipeType: SwipeType = SwipeType.NONE
    private var scaleFactor = 1f

    enum class SwipeType {
        NONE, BRIGHTNESS, VOLUME, SEEK
    }

    // -------------------- Brightness & Volume -------------------- //
    private var audioManager: AudioManager? = null
    private var maxVolume = 0
    private var currentVolume = 0
    private var currentBrightness = 0
    private var startBrightness = 0
    private var startVolume = 0

    // -------------------- Seek & Controls -------------------- //
    private var startPosition = 0L
    private val sensitivity = 5f
    private val edgeMargin by lazy {
        (50 * Resources.getSystem().displayMetrics.density).toInt()
    }

    // -------------------- Zoom -------------------- //
    private val maxScale = 3f


    @SuppressLint("ClickableViewAccessibility", "UnsafeOptInUsageError")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        binding = ActivityVideoPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.mainContainer)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        // PlayerView ke controller ko binding karna
        val controllerView = binding.playerView.findViewById<View>(R.id.controllerViewContainer)
        cBinding = ExoPlayerCustomControllerBinding.bind(controllerView)
        cBinding.exoProgress1.isEnabled = false

        Log.i("TAG", "isInPictureInPictureMode  Creat: $isInPictureInPictureMode")
        // 1️⃣ Check if URI is passed via intent.data (implicit intent)
        val intentUri: Uri? = intent.data

        // 2️⃣ Fallback: check if explicit string extra is passed
        val videoSource: String? = intent.getStringExtra("data")

        // Decide which to play
        val source = intentUri?.toString() ?: videoSource

        if (source != null) {
            playVideo(source)
           // playVideo("rtmp://demo.srs.com/show/live/livestream/2013") not work
            // playVideo("https://bitmovin-a.akamaihd.net/content/sintel/sintel.mpd")
            // playVideo("https://cdn.bitmovin.com/content/assets/sintel/hls/playlist.m3u8")
            // playVideo("https://cdn1.itekrfid.com/hls/local/index.m3u8")
            // playVideo("https://cam3.pc.cdn.bitgravity.com/cam/live/feed006/Saibaba/temple/feed/playlist.m3u8?e=1643673540&h=2dfe9075b59754710e3e0414299f0f09")
            // playVideo("https://sai.org.in/node/504?width=960&height=540&iframe=true")
        } else {
            // No video provided → close activity
            finish()
        }

        setupGestureDetector()

        // Audio manager for volume control
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        maxVolume = audioManager!!.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        currentVolume = audioManager!!.getStreamVolume(AudioManager.STREAM_MUSIC)

        // Set volume bar
        binding.volumeBar.progress = (currentVolume * 100 / maxVolume)

        // Set brightness bar
        currentBrightness = Settings.System.getInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS, 100)
        binding.brightnessBar.progress = currentBrightness

        binding.playerView.setOnTouchListener { _, event ->

            // Always handle scale gestures
            scaleGestureDetector.onTouchEvent(event)

            when (event.actionMasked) {

                MotionEvent.ACTION_DOWN -> {
                    if(!isLock){
                        if (event.pointerCount == 1) {
                            isMultiTouch = false
                            activePointerId = event.getPointerId(0)
                            startX = event.x
                            startY = event.y
                            isSwiping = false
                            swipeType = SwipeType.NONE
                            startBrightness = binding.brightnessBar.progress
                            startVolume = binding.volumeBar.progress
                            startPosition = player?.currentPosition ?: 0L
                        }
                    }
                }

                MotionEvent.ACTION_POINTER_DOWN -> {
                    // Multi-touch started
                    if(!isLock){
                        isMultiTouch = true
                    }
                }

                MotionEvent.ACTION_MOVE -> {
                    if (isMultiTouch || isLock) {
                        // Multi-touch active → ignore single-finger gestures
                        return@setOnTouchListener true
                    }

                    // Single-finger gestures only
                    if (event.pointerCount == 1 && activePointerId != MotionEvent.INVALID_POINTER_ID) {
                        val pointerIndex = event.findPointerIndex(activePointerId)
                        if (pointerIndex == -1) return@setOnTouchListener true // ✅ Pointer no longer valid

                        val x = event.getX(pointerIndex)
                        val y = event.getY(pointerIndex)

                        if (!isSwiping) {
                            val deltaX = abs(x - startX)
                            val deltaY = abs(y - startY)

                            val isInSafeArea =
                                x > edgeMargin &&
                                        x < binding.playerView.width - edgeMargin &&
                                        y > edgeMargin &&
                                        y < binding.playerView.height - edgeMargin

                            val touchSlop = ViewConfiguration.get(this).scaledTouchSlop
                            if (isInSafeArea && (deltaX > touchSlop || deltaY > touchSlop)) {
                                isSwiping = true
                                swipeType = when {
                                    deltaX > deltaY * 2 -> {
                                        binding.seekIndicator.visibility = View.VISIBLE
                                        SwipeType.SEEK
                                    }
                                    startX < binding.playerView.width / 2 -> {
                                        binding.brightnessIndicator.visibility = View.VISIBLE
                                        SwipeType.BRIGHTNESS
                                    }
                                    else -> {
                                        binding.volumeIndicator.visibility = View.VISIBLE
                                        SwipeType.VOLUME
                                    }
                                }
                            }
                        }

                        if (isSwiping) {
                            when (swipeType) {
                                SwipeType.BRIGHTNESS -> setBrightness(startY - y)
                                SwipeType.VOLUME -> startVolume(startY - y)
                                SwipeType.SEEK -> {
                                    val deltaX = x - startX
                                    val duration = player?.duration ?: 0L
                                    val proportion = deltaX / binding.playerView.width
                                    val seekTime = (proportion * duration).toLong()
                                    val newPosition = (startPosition + seekTime).coerceIn(0, duration)

                                    val totalSec = newPosition / 1000
                                    val h = totalSec / 3600
                                    val m = (totalSec % 3600) / 60
                                    val s = totalSec % 60
                                    binding.seekIndicator.text = if (h > 0) {
                                        String.format(getString(R.string.d_02d_02d), h, m, s)
                                    } else {
                                        String.format(getString(R.string._02d_02d), m, s)
                                    }
                                }
                                else -> {}
                            }
                        }
                    }
                }

                MotionEvent.ACTION_POINTER_UP -> {
                    if(!isLock){
                        // If one of multiple fingers lifted
                        if (event.pointerCount <= 2) {
                            isMultiTouch = false
                            // Reset single-finger start positions
                            val pointerIndex = event.findPointerIndex(event.getPointerId(0))
                            startX = event.getX(pointerIndex)
                            startY = event.getY(pointerIndex)
                            startPosition = player?.currentPosition ?: 0L
                            startBrightness = binding.brightnessBar.progress
                            startVolume = binding.volumeBar.progress
                            isSwiping = false
                        }
                    }
                }

                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    if(!isLock){
                        isMultiTouch = false
                        activePointerId = MotionEvent.INVALID_POINTER_ID

                        if (isSwiping && swipeType == SwipeType.SEEK) {
                            val deltaX = event.x - startX
                            val duration = player?.duration ?: 0L
                            val proportion = deltaX / binding.playerView.width
                            val seekTime = (proportion * duration).toLong()
                            val newPosition = (startPosition + seekTime).coerceIn(0, duration)
                            player?.seekTo(newPosition)
                        }

                        // Hide indicators
                        binding.brightnessIndicator.visibility = View.INVISIBLE
                        binding.volumeIndicator.visibility = View.INVISIBLE
                        binding.seekIndicator.visibility = View.INVISIBLE
                        stopFastSeek()
                    }


                    // Single tap toggle controller
                   /* if (!isSwiping) {
                        if (binding.playerView.isControllerFullyVisible) {
                            binding.playerView.hideController()
                        } else {
                            binding.playerView.showController()
                        }
                    }*/
                }
            }

            // 👇 GestureDetector को भी event दो
            gestureDetector.onTouchEvent(event)

            true
        }

        extraHandler()

        onBackPressedDispatcher.addCallback(this) {
            if (!isFullscreen) {
                // Normal back → finish activity
                finish()
            } else {
                // If fullscreen → exit fullscreen instead of finishing
                toggleFullscreen()
            }
        }
    }

    private fun extraHandler(){
        binding.playerView.findViewById<ImageButton>(R.id.exo_fullscreen_exit).setOnClickListener {
            toggleFullscreen()
        }
        binding.playerView.findViewById<ImageButton>(R.id.exo_quality).setOnClickListener {
            showQualitySelectionDialog()
        }
        binding.playerView.findViewById<ImageButton>(R.id.exo_at).setOnClickListener {
            showTrackSelectionDialog()
        }
        binding.playerView.findViewById<ImageView>(R.id.openLock).setOnClickListener {
            binding.playerView.findViewById<ConstraintLayout>(R.id.controllerView).visibility = View.GONE
            binding.playerView.findViewById<ConstraintLayout>(R.id.lockOverlay).visibility = View.VISIBLE
            isLock = true
            // binding.playerView.setOnTouchListener { _, _ -> true }
        }
        binding.playerView.findViewById<ImageView>(R.id.lockIcon).setOnClickListener {
            binding.playerView.findViewById<ConstraintLayout>(R.id.controllerView).visibility = View.VISIBLE
            binding.playerView.findViewById<ConstraintLayout>(R.id.lockOverlay).visibility = View.GONE
            isLock = false
            //  binding.playerView.setOnTouchListener(null)
        }

        binding.playerView.findViewById<ImageButton>(R.id.exo_playback_speed1).setOnClickListener {
            val bottomSheet = PlaybackSpeedBottomSheet(player!!)
            bottomSheet.show(supportFragmentManager, "PlaybackSpeedBottomSheet")

        }
        binding.playerView.findViewById<ImageButton>(R.id.exo_more).setOnClickListener {
            val sheet = PlayerOptionsBottomSheet(player!!, binding.playerView) {
                PlaybackSpeedBottomSheet(player!!).show(supportFragmentManager, "PlaybackSpeedBottomSheet")
            }
            sheet.show(supportFragmentManager, "PlayerOptionsBottomSheet")
        }
        binding.playerView.findViewById<ImageButton>(R.id.exo_back).setOnClickListener {
            if (!isFullscreen){
                onBackPressedDispatcher.onBackPressed()
            }else{
                toggleFullscreen()
            }
        }
        cBinding.exoMini.setOnClickListener {
            enterPipMode()
        }

    }

    private fun enterPipMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val pipParams = PictureInPictureParams.Builder()
                .setAspectRatio(Rational(16, 9))
                .build()
            enterPictureInPictureMode(pipParams)
        }
    }

    private fun playVideo(source: String) {
        uri = when {
            source.startsWith("http") -> Uri.parse(source)            // Remote streaming URL
            source.startsWith("https") -> Uri.parse(source)            // Remote streaming URL
            source.startsWith("file") -> Uri.fromFile(File(source.removePrefix("file://")))  // Local file path
            source.startsWith("content") -> Uri.parse(source)         // Content URI
            else -> Uri.fromFile(File(source))
        }

        // Extract just the file name
        val fileName = if (source.startsWith("http")) {
            // For URL, take last segment after "/"
            uri?.lastPathSegment ?: "Unknown"
        } else {
            File(uri?.path ?: "").name
        }

        cBinding.title.text = fileName
        cBinding.title.isSelected = true
        if (player == null) {
            initializePlayer()
        }
    }

    private fun initializePlayer() {
        val httpDataSourceFactory  = DefaultHttpDataSource.Factory()
            .setUserAgent(
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
                        "AppleWebKit/537.36 (KHTML, like Gecko) " +
                        "Chrome/118.0.0.0 Safari/537.36")

        // IMA Ads Loader
       // val adsLoader = ImaAdsLoader.Builder(this).build()

        // DefaultDataSourceFactory automatically handles file:// and http://
        val dataSourceFactory = DefaultDataSource.Factory(this,
            httpDataSourceFactory
        )

        val mediaSourceFactory = DefaultMediaSourceFactory(dataSourceFactory)

        val renderersFactory = DefaultRenderersFactory(this)
            .setEnableDecoderFallback(true)

        player = ExoPlayer.Builder(this, renderersFactory)
            .setMediaSourceFactory(mediaSourceFactory)
            .build()
        binding.playerView.player = player
        //  binding.playerView.controllerShowTimeoutMs = 0
        binding.playerView.setTimeBarScrubbingEnabled(false)

        // Set the AdsLoader to the player
       // adsLoader.setPlayer(player)

        val metadata = MediaMetadata.Builder()
            .setTitle(cBinding.title.text.toString())
            .setArtist(cBinding.title.text.toString())
            .build()

        val mediaItem = MediaItem.Builder()
           // .setUri(uri!!)
            .setUri(uri!!)
            .setMediaMetadata(metadata)
           /* .setAdsConfiguration(
                MediaItem.AdsConfiguration.Builder(
                    Uri.parse("https://pubads.g.doubleclick.net/gampad/ads?sz=640x480&iu=/124319096/external/single_ad_samples&impl=s&gdfp_req=1&env=vp&output=vast")
                ).build()
            )*/
            .build()

        player?.setMediaItem(mediaItem)
        player?.playWhenReady = playWhenReady
        player?.seekTo(currentWindow, playbackPosition)
        player?.prepare()

        mediaSession = MediaSession.Builder(this, player!!)
            .setCallback(object : MediaSession.Callback {}) // optional
            .build()

        player?.addListener(object : Player.Listener {
            override fun onEvents(player: Player, events: Player.Events) {
                // Update progress bar
                val position = player.currentPosition
                val duration = player.duration.coerceAtLeast(0)
                cBinding.exoProgress1.setDuration(duration)
                cBinding.exoProgress1.setPosition(position)
            }
            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_READY -> {
                        // Player is ready
                    }
                    Player.STATE_BUFFERING -> {
                        // Buffering
                    }
                    Player.STATE_ENDED -> {
                        // Video ended
                    }
                    Player.STATE_IDLE -> {

                    }
                }
            }
        })
    }

    private fun startFastSeek(isForward: Boolean) {
        val speed = if (isForward) 1.5f  else 0.5f
        player?.playbackParameters = PlaybackParameters(speed, 1f)

        val imageRes = if (isForward) R.drawable.ic_fastforward else R.drawable.ic_slow_motion_video
        binding.fastSeekIndicator.apply {
            setImageResource(imageRes)
            visibility = View.VISIBLE
        }
    }
    private fun stopFastSeek() {
        if (!isLongPress) return
        isLongPress = false

        // Reset playback speed
        player?.playbackParameters = PlaybackParameters(1f, 1f)
        // Hide indicator
        binding.fastSeekIndicator.visibility = View.GONE
    }

    private fun setupGestureDetector() {
        gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onLongPress(e: MotionEvent) {
                super.onLongPress(e)
                isLongPress = true
                // Zone check: left / right
                val width = binding.playerView.width
                val x = e.x
                val leftBoundary = width / 3
                val rightBoundary = 2 * width / 3

                when {
                    x < leftBoundary -> {}
                    x > rightBoundary -> {}
                    else -> {
                        // ✅ Center zone split into 2 equal halves
                        val centerWidth = rightBoundary - leftBoundary
                        val centerLeftBoundary = leftBoundary + centerWidth / 2

                        if (x < centerLeftBoundary) {
                            // Center-left → fast rewind
                            startFastSeek(isForward = false)
                        } else {
                            // Center-right → fast forward
                            startFastSeek(isForward = true)
                        }
                    }
                }
            }
            override fun onDoubleTap(e: MotionEvent): Boolean {

                val width = binding.playerView.width
                val x = e.x

                val leftBoundary = width / 3
                val rightBoundary = 2 * width / 3

                when {
                    x < leftBoundary -> {
                        // 👈 Left → rewind
                        val rewindMs = 5_000L
                        val newPos = (player?.currentPosition ?: 0L) - rewindMs
                        player?.seekTo(newPos.coerceAtLeast(0))

                        // Show overlay
                        binding.leftRewindOverlay.visibility = View.VISIBLE
                        binding.ivRewind.setImageResource(R.drawable.baseline_fast_rewind_24) // optional update text/icon
                        binding.leftRewindOverlay.alpha = 0f
                        binding.leftRewindOverlay.animate().alpha(1f).setDuration(100).withEndAction {
                            binding.leftRewindOverlay.postDelayed({
                                binding.leftRewindOverlay.animate().alpha(0f).setDuration(300).withEndAction {
                                    binding.leftRewindOverlay.visibility = View.GONE
                                }
                            }, 500)
                        }
                    }

                    x > rightBoundary -> {
                        // 👉 Right → forward
                        val forwardMs = 10_000L
                        val newPos = (player?.currentPosition ?: 0L) + forwardMs
                        val duration = player?.duration ?: 0L
                        player?.seekTo(newPos.coerceAtMost(duration))

                        // Show overlay
                        binding.rightForwardOverlay.visibility = View.VISIBLE
                        binding.ivForward.setImageResource(R.drawable.ic_fastforward)
                        binding.rightForwardOverlay.alpha = 0f
                        binding.rightForwardOverlay.animate().alpha(1f).setDuration(100).withEndAction {
                            binding.rightForwardOverlay.postDelayed({
                                binding.rightForwardOverlay.animate().alpha(0f).setDuration(300).withEndAction {
                                    binding.rightForwardOverlay.visibility = View.GONE
                                }
                            }, 500)
                        }
                    }

                    else -> {
                         togglePlayPause()
                    }
                }
                return true
            }
            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                // अब controller toggle सिर्फ single-tap पर होगा
                if (binding.playerView.isControllerFullyVisible) {
                    binding.playerView.hideController()
                } else {
                    binding.playerView.showController()
                }
                return true
            }
        })
        scaleGestureDetector = ScaleGestureDetector(this,
            object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
                override fun onScale(detector: ScaleGestureDetector): Boolean {
                    // Update the scale factor based on the gesture
                    scaleFactor *= detector.scaleFactor
                    scaleFactor = scaleFactor.coerceIn(1.0f, maxScale)

                    // Step 1: Check if we are in the initial zoom mode
                    if (!isZoomed) {
                        isZoomed = true
                        binding.playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    }

                    // Step 2: Apply the continuous scale on top of the zoom mode
                    binding.playerView.videoSurfaceView?.let { view ->
                        if (view is TextureView) {
                            val matrix = Matrix()
                            val pivotX = view.width / 2f
                            val pivotY = view.height / 2f
                            matrix.setScale(scaleFactor, scaleFactor, pivotX, pivotY)
                            view.setTransform(matrix)
                        }
                    }

                    // Reset logic: If the user pinches all the way back, reset everything
                    if (scaleFactor <= 1.0f) {
                        isZoomed = false
                        scaleFactor = 1.0f
                        binding.playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                        binding.playerView.videoSurfaceView?.let { view ->
                            if (view is TextureView) {
                                view.setTransform(Matrix()) // Reset the matrix
                            }
                        }
                    }
                    return true
                }
            })
    }

    private fun togglePlayPause() {
        player?.let {
            if (it.isPlaying) {
                it.pause()
            } else {
                it.play()
            }
        }
    }

    private fun setBrightness(diff: Float) {
        val change = (diff / sensitivity).toInt()
        val newBrightness = (startBrightness  + change).coerceIn(0, 100)
        binding.brightnessBar.progress = newBrightness
        val layoutParams = window.attributes
        layoutParams.screenBrightness = newBrightness / 100f
        window.attributes = layoutParams
    }
    private fun startVolume(delta: Float) {
        val change = (delta / sensitivity).toInt()
        val newVolume = (startVolume + change).coerceIn(0, 100)
        binding.volumeBar.progress = newVolume
        val systemVolume = (binding.volumeBar.progress * maxVolume) / 100
        audioManager!!.setStreamVolume(AudioManager.STREAM_MUSIC, systemVolume, 0)
    }

    private fun toggleFullscreen() {
        if (isFullscreen) {
            exitFullscreen()
        } else {
            enterFullscreen()
        }
    }

    private fun enterFullscreen() {
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        supportActionBar?.hide()

        // Hide system bars (status + nav)
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController.hide(WindowInsetsCompat.Type.systemBars())
        insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        // Change fullscreen button icon
        val btn = binding.playerView.findViewById<ImageButton>(R.id.exo_fullscreen_exit)
        btn.setImageResource(R.drawable.ic_fullscreen_exit)

        isFullscreen = true
    }

    private fun exitFullscreen() {
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        supportActionBar?.hide()

        // Show system bars
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController.show(WindowInsetsCompat.Type.systemBars())

        // Change fullscreen button icon
        val btn = binding.playerView.findViewById<ImageButton>(R.id.exo_fullscreen_exit)
        btn.setImageResource(R.drawable.ic_fullscreen)

        isFullscreen = false
    }

    @SuppressLint("DefaultLocale")
    private fun showQualitySelectionDialog() {
        val player = player ?: return

        val currentTracks = player.currentTracks
        val videoTrackGroup: Tracks.Group? =
            currentTracks.groups.find { it.type == C.TRACK_TYPE_VIDEO }

        if (videoTrackGroup == null || videoTrackGroup.length == 0) return

        val popupMenu = PopupMenu(this, binding.playerView.findViewById(R.id.exo_quality))

        // Get current selection
        val currentOverride = player.trackSelectionParameters
            .overrides[videoTrackGroup.mediaTrackGroup]

        // ---- Auto Option ----
        val autoMenuItem = popupMenu.menu.add("Auto")
        if (currentOverride == null) {
            // highlight Auto if no override
            val spannable = SpannableString("Auto")
            spannable.setSpan(ForegroundColorSpan(Color.RED), 0, spannable.length, 0)
            autoMenuItem.title = spannable
        }
        autoMenuItem.setOnMenuItemClickListener {
            val newParams = player.trackSelectionParameters.buildUpon()
                .clearOverridesOfType(C.TRACK_TYPE_VIDEO)
                .build()
            player.trackSelectionParameters = newParams
            true
        }

        // ---- Quality Options ----
        for (i in 0 until videoTrackGroup.length) {
            val format = videoTrackGroup.getTrackFormat(i)
            val qualityLabel = getQualityLabel(format.height)
            val bitrateMbps = if (format.bitrate > 0) {
                String.format("%.1fMbps", format.bitrate / 1_000_000.0)
            } else {
                ""
            }
            val menuText = if (bitrateMbps.isNotEmpty()) "$qualityLabel ($bitrateMbps)" else qualityLabel

            val menuItem: MenuItem = popupMenu.menu.add(menuText)

            // Highlight currently selected quality
            if (currentOverride?.trackIndices?.contains(i) == true) {
                val spannable = SpannableString(menuText)
                spannable.setSpan(ForegroundColorSpan(Color.RED), 0, spannable.length, 0)
                menuItem.title = spannable
            }

            menuItem.setOnMenuItemClickListener {
                val override = TrackSelectionOverride(videoTrackGroup.mediaTrackGroup, i)
                val newParams = player.trackSelectionParameters.buildUpon()
                    .setOverrideForType(override)
                    .build()
                player.trackSelectionParameters = newParams
                true
            }
        }

        popupMenu.show()
    }

    private fun getQualityLabel(height: Int): String {
        return when {
            height <= 144 -> "144p"
            height <= 240 -> "240p"
            height <= 360 -> "360p"
            height <= 480 -> "480p"
            height <= 720 -> "720p"
            height <= 1080 -> "1080p"
            height <= 1440 -> "2K"
            height <= 2160 -> "4K"
            else -> "${height}p"
        }
    }

    private fun showTrackSelectionDialog() {
        val trackDialog = TrackSelectionDialogBuilder(
            this,
            "Subtitle Tracks",
            player!!,
            C.TRACK_TYPE_TEXT
        ).build()
        trackDialog.show()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        val paramsRight = binding.rightForwardOverlay.layoutParams
        val paramsLest = binding.leftRewindOverlay.layoutParams

        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            // Landscape
            paramsRight.width = resources.getDimensionPixelSize(R.dimen.forward_overlay_land_width)
            paramsRight.height = resources.getDimensionPixelSize(R.dimen.forward_overlay_land_height)
            paramsLest.width = resources.getDimensionPixelSize(R.dimen.forward_overlay_land_width)
            paramsLest.height = resources.getDimensionPixelSize(R.dimen.forward_overlay_land_height)
        } else {
            // Portrait
            paramsRight.width = resources.getDimensionPixelSize(R.dimen.forward_overlay_port_width)
            paramsRight.height = resources.getDimensionPixelSize(R.dimen.forward_overlay_port_height)
            paramsLest.width = resources.getDimensionPixelSize(R.dimen.forward_overlay_port_width)
            paramsLest.height = resources.getDimensionPixelSize(R.dimen.forward_overlay_port_height)
        }

        binding.rightForwardOverlay.layoutParams = paramsRight
        binding.leftRewindOverlay.layoutParams = paramsLest
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)

        Log.i("TAG", "onPictureInPictureModeChanged: $isInPictureInPictureMode")
        Log.i("TAG", "onPictureInPictureModeChanged: $newConfig")

        if (isInPictureInPictureMode) {
            cBinding.controllerViewContainer.visibility = View.GONE
        } else {
            cBinding.controllerViewContainer.visibility = View.VISIBLE
        }
    }

    override fun onResume() {
        super.onResume()
        if (!isInPictureInPictureMode) {
            player?.play()
        }
        Log.i("TAG", "isInPictureInPictureMode  resume: $isInPictureInPictureMode")
    }

    override fun onPause() {
        super.onPause()
        if (!isInPictureInPictureMode) {
            player?.pause()
        }
        Log.i("TAG", "isInPictureInPictureMode  onPause: $isInPictureInPictureMode")
    }

    override fun onStop() {
        super.onStop()
        if (isInPictureInPictureMode) {
            player?.pause()
        }
        Log.i("TAG", "isInPictureInPictureMode  onStop: $isInPictureInPictureMode")
    }


    override fun onDestroy() {
        releasePlayer()
        Log.i("TAG", "isInPictureInPictureMode  onDestroy: $isInPictureInPictureMode")
        super.onDestroy()
    }

    private fun releasePlayer() {
        mediaSession?.release()
        player?.let {
            playbackPosition = it.currentPosition
            currentWindow = it.currentMediaItemIndex
            playWhenReady = it.playWhenReady
            it.release()
        }
        player = null
    }
}