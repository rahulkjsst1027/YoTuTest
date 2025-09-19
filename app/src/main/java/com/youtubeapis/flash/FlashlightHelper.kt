package com.youtubeapis.flash

import android.content.Context
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log

class FlashlightHelper(private val context: Context) {
    private var cameraId: String? = null
    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    private val handler = Handler(Looper.getMainLooper())
    private var isBlinking = false
    private var isBrightnessMode = false   // 🔥 new flag

    init {
        try {
            // Get first camera with flash
            cameraId = cameraManager.cameraIdList.firstOrNull {
                cameraManager.getCameraCharacteristics(it)
                    .get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }
            if (cameraId == null) {
                Log.e("FlashlightHelper", "❌ No camera with flash available on this device")
            }
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

     fun setFlash(on: Boolean) {
        cameraId?.let {
            try {
                cameraManager.setTorchMode(it, on)
            } catch (e: CameraAccessException) {
                e.printStackTrace()
            }
        }
    }

    fun startPattern(pattern: List<Long>) {
        stopPattern()
        isBlinking = true
        blinkPattern(pattern, 0)
    }

    private fun blinkPattern(pattern: List<Long>, index: Int) {
        if (!isBlinking || pattern.isEmpty()) return
        val delay = pattern[index % pattern.size]
        setFlash(index % 2 == 0)
        handler.postDelayed({
            blinkPattern(pattern, index + 1)
        }, delay)
    }

    fun stopPattern() {
        isBlinking = false
        isBrightnessMode = false  // stop brightness mode too
        handler.removeCallbacksAndMessages(null)
        setFlash(false)
    }

    /**
     * Brightness Control (0 = off, 1–10 = torch levels)
     */
    fun setBrightness(level: Int) {
        stopPattern()
        isBrightnessMode = true

        val id = cameraId ?: run {
            Log.e("FlashlightHelper", "❌ No flash available")
            return
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val maxLevel = cameraManager
                    .getCameraCharacteristics(id)
                    .get(CameraCharacteristics.FLASH_INFO_STRENGTH_MAXIMUM_LEVEL) ?: 1

                Log.i("FlashlightHelper", "Max Torch Level = $maxLevel")

                if (maxLevel > 1) {
                    // Agar device multiple levels support karta hai
                    val safeLevel = level.coerceIn(1, maxLevel)
                    cameraManager.turnOnTorchWithStrengthLevel(id, safeLevel)
                    Log.d("FlashlightHelper", "Torch ON with strength: $safeLevel / $maxLevel")
                } else {
                    // Agar sirf ON/OFF support karta hai
                    if (level <= 0) {
                        setFlash(false)
                    } else {
                        setFlash(true)
                    }
                    Log.w("FlashlightHelper", "⚠️ Torch strength not supported, falling back to ON/OFF")
                }
            } else {
                // Old Android versions
                if (level <= 0) {
                    setFlash(false)
                } else {
                    setFlash(true)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


}
