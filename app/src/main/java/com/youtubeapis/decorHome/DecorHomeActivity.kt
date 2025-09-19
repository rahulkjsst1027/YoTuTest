package com.youtubeapis.decorHome

import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.youtubeapis.databinding.ActivityDecorHomeBinding
import kotlin.math.log10
import kotlin.math.sqrt

class DecorHomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDecorHomeBinding
    private var isMeasuring = false
    private lateinit var audioRecord: AudioRecord

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityDecorHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnCalculate.setOnClickListener {
            val houseLength = binding.etHouseLength.text.toString().toDoubleOrNull() ?: 0.0
            val gap = binding.etGap.text.toString().toDoubleOrNull() ?: 0.0
            val hang = binding.etHang.text.toString().toDoubleOrNull() ?: 0.0

            if (houseLength > 0 && gap > 0 && hang > 0) {
                val (feet, meter, cm) = calculateTotalStringLength(
                    houseLengthFeet = houseLength,
                    gapFeet = gap,
                    hangFeet = hang
                )
                val result = """
                    Total String Length:
                    ${"%.2f".format(feet)} feet
                    ${"%.2f".format(meter)} meter
                    ${"%.2f".format(cm)} cm
                """.trimIndent()

                binding.tvResult.text = result
            } else {
                binding.tvResult.text = "Please enter valid values!"
            }
        }

        binding.btnCalculatePaint.setOnClickListener {
            val lengthInput = binding.etLength.text.toString().toDoubleOrNull() ?: 0.0
            val widthInput = binding.etWidth.text.toString().toDoubleOrNull() ?: 0.0
            val heightInput = binding.etHeight.text.toString().toDoubleOrNull() ?: 0.0

            val selectedUnit = binding.spinnerUnits.selectedItem.toString()
            val selectedPaintType = binding.spinnerPaintType.selectedItem.toString()

            // Convert all inputs to meters
            val length = convertToMeters(lengthInput, selectedUnit)
            val width = convertToMeters(widthInput, selectedUnit)
            val height = convertToMeters(heightInput, selectedUnit)

            // Set coverage based on paint type
            val coveragePerLiter = when {
                selectedPaintType.contains("Primer") -> 8.0
                selectedPaintType.contains("Oil") -> 6.0
                else -> 10.0 // Default Emulsion/Latex
            }

            if (length > 0 && width > 0 && height > 0) {
                val wallArea = 2 * (length * height + width * height)
                val ceilingArea = if (binding.cbIncludeCeiling.isChecked) length * width else 0.0
                val totalArea = wallArea + ceilingArea

                val litersNeeded = totalArea / coveragePerLiter

                val result = """
                    Unit Selected: $selectedUnit
                    Walls Area: ${"%.2f".format(wallArea)} m²
                    Ceiling Area: ${"%.2f".format(ceilingArea)} m²
                    -------------------------
                    Total Area: ${"%.2f".format(totalArea)} m²
                    Paint Needed: ${"%.2f".format(litersNeeded)} liters
                """.trimIndent()

                binding.tvPaintResult.text = result
            } else {
                binding.tvPaintResult.text = "⚠️ Please enter valid values!"
            }
        }

        binding.btnStart.setOnClickListener {
            when {
                ContextCompat.checkSelfPermission(this,
                    Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED -> {
                    // already granted
                    startMeasuring()
                    binding.btnStart.visibility = View.GONE
                    binding.btnStop.visibility = View.VISIBLE
                }
                else -> {
                    // request karo
                    micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }
            }
        }

        binding.btnStop.setOnClickListener {
            stopMeasuring()
            binding.btnStart.visibility = View.VISIBLE
            binding.btnStop.visibility = View.GONE
        }

    }

    // 👉 permission launcher banate hi ready ho jaayega
    private val micPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // User ne allow kar diya
            startMeasuring()
            binding.btnStart.visibility = View.GONE
            binding.btnStop.visibility = View.VISIBLE
        } else {
            // Denied
            Toast.makeText(this, "Microphone permission required!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun startMeasuring() {
        val sampleRate = 44100
        val bufferSize = AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )

        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            return
        }
        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSize
        )

        audioRecord.startRecording()
        isMeasuring = true

        Thread {
            val buffer = ShortArray(bufferSize)
            while (isMeasuring) {
                val read = audioRecord.read(buffer, 0, buffer.size)
                if (read > 0) {
                    var sum = 0.0
                    for (i in 0 until read) {
                        sum += buffer[i] * buffer[i]
                    }
                    val rms = sqrt(sum / read)
                    val db = 20 * log10(rms / 32767.0) // normalize PCM range

                    runOnUiThread {
                        val safeLevel = when {
                            db < 30 -> "Very Quiet ✅"
                            db < 60 -> "Normal Room Level 🙂"
                            db < 85 -> "Noisy ⚠️"
                            else -> "Dangerous 🚨 (Hearing Risk)"
                        }
                        binding.tvNoiseLevel.text =
                            "Noise Level: ${"%.2f".format(db)} dB\n$safeLevel"
                    }
                }
            }
        }.start()
    }

    private fun stopMeasuring() {
        isMeasuring = false
        audioRecord.stop()
        audioRecord.release()
    }

    private fun convertToMeters(value: Double, unit: String): Double {
        return when (unit) {
            "Feet" -> value * 0.3048   // 1 ft = 0.3048 m
            "Inches" -> value * 0.0254 // 1 in = 0.0254 m
            else -> value              // already in meters
        }
    }

    private fun calculateTotalStringLength(
        houseLengthFeet: Double,
        gapFeet: Double,
        hangFeet: Double
    ): Triple<Double, Double, Double> {
        // kitne points lagenge
        val points = (houseLengthFeet / gapFeet).toInt()

        // ek section ki length (pythagoras)
        val sectionLength = sqrt(gapFeet * gapFeet + hangFeet * hangFeet)

        // total length feet me
        val totalFeet = points * sectionLength

        // conversion
        val totalMeter = totalFeet * 0.3048
        val totalCm = totalMeter * 100

        return Triple(totalFeet, totalMeter, totalCm)
    }
}