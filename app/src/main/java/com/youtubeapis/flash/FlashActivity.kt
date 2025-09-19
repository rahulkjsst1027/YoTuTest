package com.youtubeapis.flash

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.SeekBar
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.youtubeapis.R

class FlashActivity : AppCompatActivity() {

    private lateinit var flashlight: FlashlightHelper
    private lateinit var patterns: FlashlightPatterns

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_flash)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        flashlight = FlashlightHelper(this)
        patterns = FlashlightPatterns(flashlight)

        findViewById<Button>(R.id.btnBlink).setOnClickListener { patterns.blink() }
        findViewById<Button>(R.id.btnFastBlink).setOnClickListener { patterns.fastBlink() }
        findViewById<Button>(R.id.btnSlowBlink).setOnClickListener { patterns.slowBlink() }
        findViewById<Button>(R.id.btnSOS).setOnClickListener { patterns.sos() }
        findViewById<Button>(R.id.btnStrobe).setOnClickListener { patterns.strobe() }
        findViewById<Button>(R.id.btnPolice).setOnClickListener { patterns.police() }
        findViewById<Button>(R.id.btnHeartbeat).setOnClickListener { patterns.heartbeat() }
        findViewById<Button>(R.id.btnBreathing).setOnClickListener { patterns.breathing() }
        findViewById<Button>(R.id.btnRandom).setOnClickListener { patterns.randomBlink() }
        findViewById<Button>(R.id.btnEmergency).setOnClickListener { patterns.emergency() }
        findViewById<Button>(R.id.btnCountdown).setOnClickListener { patterns.countdown(10) }
        findViewById<Button>(R.id.btnMorse).setOnClickListener { patterns.morseCode("SOS HELP") }
        findViewById<Button>(R.id.btnStop).setOnClickListener { patterns.stop() }
        findViewById<Button>(R.id.btnOn).setOnClickListener { patterns.on() }

        findViewById<Button>(R.id.call).setOnClickListener {
            val intent = Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
            startActivity(intent)
        }

        val seekBar = findViewById<SeekBar>(R.id.seekBar)
        seekBar.max = 5   // kyunki progress 0–9 hoga → +1 = 1–10
        seekBar.progress = 0  // default = 5 brightness

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val level = progress // snap 0..9 → 1..10
                flashlight.setBrightness(level)
                Log.d("Flash", "Brightness Level = $level")
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

    }
}