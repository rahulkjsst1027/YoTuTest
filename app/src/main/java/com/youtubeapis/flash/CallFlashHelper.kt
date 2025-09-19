package com.youtubeapis.flash

import android.content.Context
import android.os.Build
import android.telephony.PhoneStateListener
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import androidx.annotation.RequiresApi

class CallFlashHelper(
    private val context: Context,
    private val flashlight: FlashlightHelper
) {
    private val telephonyManager =
        context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

    @Suppress("DEPRECATION")
    private val oldListener = object : PhoneStateListener() {
        @Deprecated("Deprecated in Java")
        override fun onCallStateChanged(state: Int, phoneNumber: String?) {
            when (state) {
                TelephonyManager.CALL_STATE_RINGING -> startBlink()
                TelephonyManager.CALL_STATE_OFFHOOK,
                TelephonyManager.CALL_STATE_IDLE -> stopBlink()
            }
        }
    }

    private var blinking = false
    private var thread: Thread? = null

    fun register() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            telephonyManager.registerTelephonyCallback(
                context.mainExecutor,
                callListener
            )
        } else {
            telephonyManager.listen(oldListener, PhoneStateListener.LISTEN_CALL_STATE)
        }
    }

    fun unregister() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            telephonyManager.unregisterTelephonyCallback(callListener)
        } else {
            telephonyManager.listen(oldListener, PhoneStateListener.LISTEN_NONE)
        }
        stopBlink()
    }

    private fun startBlink() {
        if (blinking) return
        blinking = true
        thread = Thread {
            try {
                while (blinking) {
                    flashlight.setFlash(true)
                    Thread.sleep(200)
                    flashlight.setFlash(false)
                    Thread.sleep(200)
                }
            } catch (_: InterruptedException) {
            }
        }
        thread?.start()
    }

    private fun stopBlink() {
        blinking = false
        thread?.interrupt()
        flashlight.setFlash(false)
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private val callListener = object : TelephonyCallback(), TelephonyCallback.CallStateListener {
        override fun onCallStateChanged(state: Int) {
            when (state) {
                TelephonyManager.CALL_STATE_RINGING -> startBlink()
                TelephonyManager.CALL_STATE_OFFHOOK,
                TelephonyManager.CALL_STATE_IDLE -> stopBlink()
            }
        }
    }
}


