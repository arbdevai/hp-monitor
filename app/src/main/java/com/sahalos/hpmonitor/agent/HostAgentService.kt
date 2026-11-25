package com.sahalos.hpmonitor.agent

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.sahalos.hpmonitor.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.net.NetworkInterface
import java.net.Socket
import java.util.Collections

/**
 * Foreground Service hosting the EmbeddedAgentServer so metrics collection
 * continues reliably even when the screen is off or app is backgrounded.
 */
class HostAgentService : Service() {

    private var agentServer: EmbeddedAgentServer? = null
    private var statsJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START
        if (action == ACTION_STOP) {
            stopServer()
            stopSelf()
            return START_NOT_STICKY
        }

        startServer()
        val notification = buildNotification("Server metrik aktif di port ${EmbeddedAgentServer.DEFAULT_PORT}")
        startForeground(NOTIFICATION_ID, notification)
        return START_STICKY
    }

    private fun startServer() {
        if (agentServer == null) {
            val server = EmbeddedAgentServer(applicationContext, EmbeddedAgentServer.DEFAULT_PORT)
            agentServer = server
            server.start()
            _isRunning.value = true
            _localIp.value = getLanIpAddress()

            statsJob?.cancel()
            statsJob = scope.launch {
                while (isActive && _isRunning.value) {
                    _requestCount.value = server.requestCount.get()
                    delay(2000)
                }
            }
        }
    }

    private fun stopServer() {
        statsJob?.cancel()
        statsJob = null
        agentServer?.stop()
        agentServer = null
        _isRunning.value = false
    }

    override fun onDestroy() {
        stopServer()
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "HP Monitor Host Agent",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Menjaga agent server tetap berjalan di latar belakang"
                setShowBadge(false)
            }
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            nm?.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(text: String): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("HP Monitor — Mode Host")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    companion object {
        const val ACTION_START = "com.sahalos.hpmonitor.action.START_AGENT"
        const val ACTION_STOP = "com.sahalos.hpmonitor.action.STOP_AGENT"
        private const val CHANNEL_ID = "hp_monitor_host_agent"
        private const val NOTIFICATION_ID = 8787

        private val _isRunning = MutableStateFlow(false)
        val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

        private val _requestCount = MutableStateFlow(0L)
        val requestCount: StateFlow<Long> = _requestCount.asStateFlow()

        private val _localIp = MutableStateFlow("")
        val localIp: StateFlow<String> = _localIp.asStateFlow()

        fun start(context: Context) {
            val intent = Intent(context, HostAgentService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, HostAgentService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }

        fun getLanIpAddress(): String {
            try {
                val s = Socket()
                s.connect(java.net.InetSocketAddress("8.8.8.8", 80), 2000)
                val ip = s.localAddress.hostAddress ?: ""
                s.close()
                if (ip.isNotEmpty() && ip != "0.0.0.0" && ip != "127.0.0.1") {
                    return ip
                }
            } catch (_: Exception) {}

            try {
                val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
                for (intf in interfaces) {
                    if (intf.isLoopback || !intf.isUp) continue
                    val addrs = Collections.list(intf.inetAddresses)
                    for (addr in addrs) {
                        if (!addr.isLoopbackAddress && addr is java.net.Inet4Address) {
                            return addr.hostAddress ?: ""
                        }
                    }
                }
            } catch (_: Exception) {}
            return "127.0.0.1"
        }
    }
}
