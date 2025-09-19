package com.youtubeapis.vpn

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.core.app.NotificationCompat
import com.youtubeapis.R
import java.io.FileInputStream
import java.io.FileOutputStream

class MyVpnService : VpnService() {

    private val TAG = "MyVpnService"

    companion object {
        const val BROADCAST_ACTION = "vpn_status_changed"
        const val ACTION_STOP = "com.youtubeapis.vpn.ACTION_STOP"

        var isVpnRunning = false

        val BLOCKED_HOSTS = listOf(
            "ads.google.com",
            "doubleclick.net",
            "tracking.example.com",
            "playstrime.media",          // example domain for media ads
            "adservice.google.com",
            "facebook.com",              // blocks some FB/Instagram ad calls
            "instagram.com",
            "fbcdn.net",                 // Facebook CDN (ads/images)
            "edge-mqtt.facebook.com"     // FB messaging / ad push

        )

    }

    @Volatile private var vpnThread: Thread? = null
    @Volatile private var isRunning = false
    @Volatile private var vpnInterface: ParcelFileDescriptor? = null

    @Volatile private var inputStream: FileInputStream? = null
    @Volatile private var outputStream: FileOutputStream? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand, action=${intent?.action}")

        // If caller asked to stop, do the clean stop path
        if (intent?.action == ACTION_STOP) {
            Log.d(TAG, "Received ACTION_STOP -> stopping VPN")
            // run stopVpn on main thread (safe)
            stopVpn()
            // no restart
            return START_NOT_STICKY
        }

        val selectedServerName = intent?.getStringExtra("serverName") ?: "LocalAdBlockVPN"
        val selectedServerIP = intent?.getStringExtra("serverIP") ?: "10.0.0.2"

        startForegroundServiceSafe(selectedServerName, selectedServerIP)

        // Make sure any previous worker is stopped before starting a new one
        stopWorkerThreadIfRunning()

        vpnThread = Thread {
            try {
                val builder = Builder()
                    .setSession(selectedServerName)
                    .addAddress(selectedServerIP, 32)
                   // .addRoute("0.0.0.0", 0)
                    .addDnsServer("8.8.8.8")  // Google DNS

              /* arrayOf("com.google.android.youtube").forEach { pkg ->
                    try {
                        builder.addAllowedApplication(pkg)
                        Log.d(TAG, "Added allowed app: $pkg")
                    } catch (e: Exception) {
                        Log.w(TAG, "Cannot add allowed app $pkg: ${e.message}")
                    }
                }*/

                vpnInterface = builder.establish()
                if (vpnInterface == null) {
                    Log.w(TAG, "Failed to establish VPN (permission denied?)")
                    return@Thread
                }

                inputStream = FileInputStream(vpnInterface!!.fileDescriptor)
                outputStream = FileOutputStream(vpnInterface!!.fileDescriptor)

                isRunning = true
                isVpnRunning = true
                sendStatusBroadcast(true)
                Log.d(TAG, "VPN established - entering loop")

                val buffer = ByteArray(32767)
                while (isRunning) {
                    val length = try {
                        inputStream?.read(buffer) ?: -1
                    } catch (e: Exception) {
                        Log.w(TAG, "read() threw: ${e.message}")
                        -1
                    }

                    if (!isRunning) break

                    if (length > 0) {
                        // simple filter demo
                        val packetStr = try { String(buffer, 0, length) } catch (t: Throwable) { "" }
                        var blocked = false
                        for (host in BLOCKED_HOSTS) {
                            if (packetStr.contains(host)) {
                                blocked = true
                                break
                            }
                        }
                        if (!blocked) {
                            try {
                                outputStream?.write(buffer, 0, length)
                            } catch (e: Exception) {
                                Log.w(TAG, "write() threw: ${e.message}")
                                break
                            }
                        } else {
                            // dropped packet
                        }
                    } else if (length == -1) {
                        Log.d(TAG, "read returned -1, breaking")
                        break
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Worker thread exception", e)
            } finally {
                Log.d(TAG, "Worker finally: cleanup")
                // ensure cleanup if thread ends naturally
                closeStreamsAndInterface()
                isRunning = false
                isVpnRunning = false
                sendStatusBroadcast(false)
            }
        }

        vpnThread?.start()
        return START_STICKY
    }

    // Public/central stop method — idempotent and safe to call from onDestroy or ACTION_STOP
    private fun stopVpn() {
        Log.d(TAG, "stopVpn() called")
        // 1) ask loop to stop
        isRunning = false

        // 2) close streams and interface ASAP to unblock read()
        closeStreamsAndInterface()

        // 3) join worker thread briefly
        vpnThread?.let {
            try {
                Log.d(TAG, "waiting for worker to finish")
                it.join(600) // wait up to 600ms
            } catch (e: InterruptedException) {
                Log.w(TAG, "join interrupted")
            }
        }
        vpnThread = null

        // 4) stop foreground now (remove notification)
        try {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } catch (e: Exception) {
            Log.w(TAG, "stopForeground threw: ${e.message}")
        }

        // 5) update flags and notify UI
        isVpnRunning = false
        sendStatusBroadcast(false)

        // 6) finally stop service
        try {
            stopSelf()
        } catch (e: Exception) {
            Log.w(TAG, "stopSelf threw: ${e.message}")
        }
    }

    private fun stopWorkerThreadIfRunning() {
        vpnThread?.let { t ->
            isRunning = false
            try { vpnInterface?.close() } catch (e: Exception) { /* ignore */ }
            try { t.join(300) } catch (e: Exception) { /* ignore */ }
            vpnThread = null
        }
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy called")
        // ensure central stop runs
        stopVpn()
        super.onDestroy()
    }

    private fun closeStreamsAndInterface() {
        try {
            inputStream?.close()
        } catch (e: Exception) { Log.w(TAG, "input close failed: ${e.message}") }
        inputStream = null

        try {
            outputStream?.close()
        } catch (e: Exception) { Log.w(TAG, "output close failed: ${e.message}") }
        outputStream = null

        try {
            vpnInterface?.close()
        } catch (e: Exception) { Log.w(TAG, "vpnInterface close failed: ${e.message}") }
        vpnInterface = null
    }

    private fun startForegroundServiceSafe(name: String, ip: String) {
        val channelId = "vpn_channel"
        val channelName = "VPN Service"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val chan = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_LOW)
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(chan)
        }
        val stopIntent = Intent(this, MyVpnService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            0,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle(name)
            .setContentText("VPN running on $ip")
            .setSmallIcon(R.drawable.marketing)
            .setOngoing(true)
            .addAction(
                R.drawable.marketing, // icon for button
                "Disconnect",             // button text
                stopPendingIntent         // PendingIntent for button
            )
            .build()
        startForeground(1, notification)
    }

    private fun sendStatusBroadcast(connected: Boolean) {
        val intent = Intent(BROADCAST_ACTION)
        intent.putExtra("connected", connected)
        sendBroadcast(intent)
    }
}
