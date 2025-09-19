package com.youtubeapis.files

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.SeekBar
import androidx.media3.exoplayer.ExoPlayer
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.youtubeapis.R
import com.youtubeapis.databinding.PlaybackSpeedBottomSheetBinding

class PlaybackSpeedBottomSheet(
    private val player: ExoPlayer
) : BottomSheetDialogFragment() {

    private var _binding: PlaybackSpeedBottomSheetBinding? = null
    private val binding get() = _binding!!

    // min & max speed
    private val minSpeed = 0.25f
    private val maxSpeed = 3.0f

   /* override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.setOnShowListener {
            val bottomSheet = dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.setBackgroundColor(Color.parseColor("#CC1E1E1E"))
            // CC = 80% opacity (adjust as needed)
        }
        return dialog
    }*/
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
        _binding = PlaybackSpeedBottomSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ✅ Get current player speed when opening
        val currentSpeed = player.playbackParameters.speed
        binding.speedSeekBar.max = 1000
        binding.speedSeekBar.progress = speedToProgress(currentSpeed)
        updateTitle(currentSpeed)
        highlightSelected(currentSpeed)

        // SeekBar listener
        binding.speedSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    val speed = progressToSpeed(progress)
                    setSpeed(speed)
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // - button
        binding.decreaseSpeed.setOnClickListener {
            val newSpeed = (player.playbackParameters.speed - 0.25f).coerceAtLeast(minSpeed)
            setAndSync(newSpeed)
        }

        // + button
        binding.increaseSpeed.setOnClickListener {
            val newSpeed = (player.playbackParameters.speed + 0.25f).coerceAtMost(maxSpeed)
            setAndSync(newSpeed)
        }

        // Speed buttons
        binding.speed1x.setOnClickListener { setAndSync(1f) }
        binding.speed125x.setOnClickListener { setAndSync(1.25f) }
        binding.speed15x.setOnClickListener { setAndSync(1.5f) }
        binding.speed2x.setOnClickListener { setAndSync(2f) }
        binding.speed3x.setOnClickListener { setAndSync(3f) }
    }

    private fun setAndSync(speed: Float) {
        setSpeed(speed)
        binding.speedSeekBar.progress = speedToProgress(speed)
        highlightSelected(speed)
    }

    private fun setSpeed(speed: Float) {
        player.setPlaybackSpeed(speed)
        updateTitle(speed)
    }

    @SuppressLint("DefaultLocale")
    private fun updateTitle(speed: Float) {
        val formatted = String.format("%.2f", speed)
        binding.title.text = "${formatted}x"
    }


    // Highlight selected button
    private fun highlightSelected(speed: Float) {
        val buttons = listOf(
            binding.speed1x to 1f,
            binding.speed125x to 1.25f,
            binding.speed15x to 1.5f,
            binding.speed2x to 2f,
            binding.speed3x to 3f
        )
        for ((btn, value) in buttons) {
            if (speed == value) {
                btn.setBackgroundResource(R.drawable.draw_round) // selected bg
            } else {
                btn.setBackgroundResource(R.drawable.draw_round) // normal bg
            }
        }
    }

    private fun progressToSpeed(progress: Int): Float {
        return minSpeed + (progress / 1000f) * (maxSpeed - minSpeed)
    }

    private fun speedToProgress(speed: Float): Int {
        return (((speed - minSpeed) / (maxSpeed - minSpeed)) * 1000).toInt()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
