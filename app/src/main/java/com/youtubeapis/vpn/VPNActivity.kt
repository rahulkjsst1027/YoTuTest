package com.youtubeapis.vpn

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.youtubeapis.databinding.ActivityVpnactivityBinding

class VPNActivity : AppCompatActivity() {

    private lateinit var binding: ActivityVpnactivityBinding

    private val serverList = listOf(
        "Local Demo 1 - 10.0.0.2",
        "Local Demo 2 - 10.0.0.3"
    )

    private val vpnPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            startVpnService()
        } else {
            Toast.makeText(this, "VPN permission denied!", Toast.LENGTH_SHORT).show()
        }
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            prepareVpn()
        } else {
            Toast.makeText(this, "Notification permission required", Toast.LENGTH_SHORT).show()
        }
    }

    private val vpnStatusReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val connected = intent?.getBooleanExtra("connected", false) ?: false
            updateUI(connected)

            // Hide progress & enable buttons
            binding.progressBar.visibility = View.GONE
           // binding.btnConnect.isEnabled = !connected
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVpnactivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Spinner for selecting server
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, serverList)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerServers.adapter = adapter

        binding.btnConnect.setOnClickListener {

            binding.progressBar.visibility = View.VISIBLE
           // binding.btnConnect.isEnabled = false

            // Notification permission for Android 13+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) !=
                    PackageManager.PERMISSION_GRANTED) {
                    notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                    return@setOnClickListener
                }
            }
            prepareVpn()
        }

        binding.btnDisconnect.setOnClickListener {
            stopVpnService()
            binding.progressBar.visibility = View.GONE
        }

        updateUI(MyVpnService.isVpnRunning)
    }

    private fun prepareVpn() {
        val intent = VpnService.prepare(this)
        if (intent != null) {
            vpnPermissionLauncher.launch(intent)
        } else {
            startVpnService()
        }
    }

    private fun startVpnService() {
        val selected = binding.spinnerServers.selectedItem.toString()
        val parts = selected.split(" - ")
        val serverName = parts[0]
        val serverIP = parts.getOrNull(1) ?: "10.0.0.2"

        val intent = Intent(this, MyVpnService::class.java)
        intent.putExtra("serverName", serverName)
        intent.putExtra("serverIP", serverIP)
        startService(intent)
        updateUI(true)
    }

    private fun stopVpnService() {
        val stopIntent = Intent(this, MyVpnService::class.java).apply {
            action = MyVpnService.ACTION_STOP
        }
        startService(stopIntent)

        updateUI(false)
    }

    private fun updateUI(isConnected: Boolean) {
        if (isConnected) {
            binding.tvStatus.text = "VPN Status: Connected"
            binding.btnConnect.visibility = View.GONE
            binding.btnDisconnect.visibility = View.VISIBLE
        } else {
            binding.tvStatus.text = "VPN Status: Disconnected"
            binding.btnConnect.visibility = View.VISIBLE
            binding.btnDisconnect.visibility = View.GONE
        }
    }

    override fun onResume() {
        super.onResume()
        val filter = IntentFilter(MyVpnService.BROADCAST_ACTION)
        ContextCompat.registerReceiver(this, vpnStatusReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        updateUI(MyVpnService.isVpnRunning)
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(vpnStatusReceiver)
    }
}
