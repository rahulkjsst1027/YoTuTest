package com.youtubeapis.flash

import android.media.audiofx.Visualizer

class MusicFlashHelper(
    private val flashlight: FlashlightHelper,
    private val audioSessionId: Int
) {
    private var visualizer: Visualizer? = null

    fun start() {
        visualizer = Visualizer(audioSessionId).apply {
            captureSize = Visualizer.getCaptureSizeRange()[1]
            setDataCaptureListener(object : Visualizer.OnDataCaptureListener {
                override fun onWaveFormDataCapture(
                    visualizer: Visualizer?,
                    waveform: ByteArray?,
                    samplingRate: Int
                ) {
                    if (waveform != null) {
                        val amplitude = waveform.maxOrNull()?.toInt() ?: 0
                        if (amplitude > 50) {
                            flashlight.setFlash(true)
                        } else {
                            flashlight.setFlash(false)
                        }
                    }
                }

                override fun onFftDataCapture(
                    visualizer: Visualizer?,
                    fft: ByteArray?,
                    samplingRate: Int
                ) {}
            }, Visualizer.getMaxCaptureRate() / 2, true, false)
            enabled = true
        }
    }

    fun stop() {
        visualizer?.release()
        visualizer = null
        flashlight.setFlash(false)
    }

   /* val exoPlayer = ExoPlayer.Builder(this).build()
    exoPlayer.setMediaItem(MediaItem.fromUri("song_uri"))
    exoPlayer.prepare()
    exoPlayer.play()

    // Flash sync
    val musicFlash = MusicFlashHelper(flashlight, exoPlayer.audioSessionId)
    musicFlash.start()*/

}
