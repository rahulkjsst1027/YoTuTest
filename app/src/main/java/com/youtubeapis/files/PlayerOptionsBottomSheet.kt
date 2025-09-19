package com.youtubeapis.files

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioManager
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.SeekBar
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.youtubeapis.databinding.ControllerBottomSheetBinding


@SuppressLint("UnsafeOptInUsageError")
class PlayerOptionsBottomSheet(
    private val player: ExoPlayer,
    private val playerView: PlayerView,
    private val onOpenModify: () -> Unit // callback to open first sheet
) : BottomSheetDialogFragment() {

    private var _binding: ControllerBottomSheetBinding? = null
    private val binding get() = _binding!!

    private lateinit var audioManager: AudioManager

    override fun onStart() {
        super.onStart()

        val dialog = dialog as? BottomSheetDialog
        val bottomSheet =
            dialog?.findViewById<FrameLayout>(com.google.android.material.R.id.design_bottom_sheet)

        bottomSheet?.let {
            val behavior = BottomSheetBehavior.from(it)

            // ✅ Direct expanded open ho
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
            behavior.skipCollapsed = true

            // ✅ Height = wrap_content
            it.layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
        }
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ControllerBottomSheetBinding.inflate(inflater, container, false)
        audioManager = requireContext().getSystemService(Context.AUDIO_SERVICE) as AudioManager
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ========== SCALE ==========
        binding.scaleFit.setOnClickListener { setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FIT) }
        binding.scaleFill.setOnClickListener { setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FILL) }
        binding.scaleCrop.setOnClickListener { setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_ZOOM) }
        binding.scale11.setOnClickListener { setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH) }

        // ========== SPEED GRID ==========
        binding.speed05.setOnClickListener { setSpeed(0.5f) }
        binding.speed075.setOnClickListener { setSpeed(0.75f) }
        binding.speed1.setOnClickListener { setSpeed(1f) }
        binding.speed125.setOnClickListener { setSpeed(1.25f) }
        binding.speed15.setOnClickListener { setSpeed(1.5f) }
        binding.speed2.setOnClickListener { setSpeed(2f) }
        binding.speed3.setOnClickListener { setSpeed(3f) }
        binding.speedModify.setOnClickListener {
            dismiss()
            onOpenModify.invoke()
        }

        // ========== VOLUME ==========
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        binding.volumeSeek.max = maxVolume
        binding.volumeSeek.progress = currentVolume

        binding.volumeSeek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, progress, 0)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        binding.volumeMute.setOnClickListener {
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0)
            binding.volumeSeek.progress = 0
        }
        binding.volumeHigh.setOnClickListener {
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, maxVolume, 0)
            binding.volumeSeek.progress = maxVolume
        }

        // ========== BRIGHTNESS ==========
        val lp = requireActivity().window.attributes
        val currentBrightness = (lp.screenBrightness.takeIf { it >= 0 } ?: getSystemBrightness()) * 100
        binding.brightnessSeek.progress = currentBrightness.toInt()

        binding.brightnessSeek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    val brightness = progress / 100f
                    lp.screenBrightness = brightness
                    requireActivity().window.attributes = lp
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        binding.brightnessLow.setOnClickListener {
            lp.screenBrightness = 0.1f
            requireActivity().window.attributes = lp
            binding.brightnessSeek.progress = 10
        }
        binding.brightnessHigh.setOnClickListener {
            lp.screenBrightness = 1f
            requireActivity().window.attributes = lp
            binding.brightnessSeek.progress = 100
        }

        // ========== INITIAL STATE ==========
        updateSelectedScale()
        updateSelectedSpeed()
    }



    private fun setSpeed(speed: Float) {
        player.setPlaybackSpeed(speed)
        updateSelectedSpeed()
    }


    private fun setResizeMode(mode: Int) {
        playerView.resizeMode = mode
        updateSelectedScale()
    }

    private fun updateSelectedScale() {
        val all = listOf(binding.scaleFit, binding.scaleFill, binding.scaleCrop, binding.scale11)
        all.forEach { it.isSelected = false }

        when (playerView.resizeMode) {
            AspectRatioFrameLayout.RESIZE_MODE_FIT -> binding.scaleFit.isSelected = true
            AspectRatioFrameLayout.RESIZE_MODE_FILL -> binding.scaleFill.isSelected = true
            AspectRatioFrameLayout.RESIZE_MODE_ZOOM -> binding.scaleCrop.isSelected = true
            AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH -> binding.scale11.isSelected = true
        }
    }



    private fun updateSelectedSpeed() {
        val speed = player.playbackParameters.speed
        val all = listOf(
            binding.speed05 to 0.5f,
            binding.speed075 to 0.75f,
            binding.speed1 to 1f,
            binding.speed125 to 1.25f,
            binding.speed15 to 1.5f,
            binding.speed2 to 2f,
            binding.speed3 to 3f
        )

        // Find closest speed button
        var minDiff = Float.MAX_VALUE
        var selectedButton: View? = null
        all.forEach { (tv, s) ->
            val diff = kotlin.math.abs(speed - s)
            if (diff < minDiff) {
                minDiff = diff
                selectedButton = tv
            }
            tv.isSelected = false
        }

        if (minDiff <= 0.01f) {
            selectedButton?.isSelected = true
            binding.speedModify.isSelected = false
        } else {
            // Agar match nahi mila, Modify ko select
            binding.speedModify.isSelected = true
        }
    }


    private fun getSystemBrightness(): Float {
        return try {
            val value = Settings.System.getInt(requireContext().contentResolver, Settings.System.SCREEN_BRIGHTNESS)
            value / 255f
        } catch (e: Exception) {
            0.5f
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
