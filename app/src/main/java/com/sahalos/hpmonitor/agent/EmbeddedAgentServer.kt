package com.sahalos.hpmonitor.agent

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

/**
 * Lightweight HTTP server running inside the Android app.
 * Exposes /metrics for the HP Monitor dashboard app.
 */
class EmbeddedAgentServer(
    private val context: Context,
    val port: Int = DEFAULT_PORT,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var serverSocket: ServerSocket? = null
    private var serverJob: Job? = null
    private val running = AtomicBoolean(false)
    val requestCount = AtomicLong(0)

    private val collector = AndroidMetricsCollector(context)
    private val json = Json {
        encodeDefaults = true
        prettyPrint = false
    }

    fun isRunning(): Boolean = running.get()

    fun start() {
        if (running.getAndSet(true)) return

        serverJob = scope.launch {
            try {
                val ss = ServerSocket(port, 50, InetAddress.getByName("0.0.0.0"))
                serverSocket = ss
                while (isActive && running.get()) {
                    try {
                        val client = ss.accept()
                        scope.launch { handleClient(client) }
                    } catch (_: Exception) {
                        break
                    }
                }
            } catch (_: Exception) {
                running.set(false)
            } finally {
                running.set(false)
            }
        }
    }

    fun stop() {
        running.set(false)
        try {
            serverSocket?.close()
        } catch (_: Exception) {}
        serverJob?.cancel()
        serverSocket = null
    }

    private fun handleClient(socket: Socket) {
        socket.use { s ->
            s.soTimeout = 4000
            val reader = BufferedReader(InputStreamReader(s.getInputStream(), Charsets.UTF_8))
            val line = reader.readLine() ?: return
            val parts = line.split(" ")
            if (parts.size < 2) return

            val method = parts[0]
            val path = parts[1]
            requestCount.incrementAndGet()

            val writer = OutputStreamWriter(s.getOutputStream(), Charsets.UTF_8)
            if (path == "/" || path == "/index.html") {
                val body = "{\"status\":\"ok\",\"service\":\"hp-monitor-agent-android\"}\n"
                sendResponse(writer, 200, "application/json", body)
            } else if (path.startsWith("/metrics")) {
                val snap = collector.collect()
                val body = json.encodeToString(snap) + "\n"
                sendResponse(writer, 200, "application/json", body)
            } else {
                val body = "{\"error\":\"not found\"}\n"
                sendResponse(writer, 404, "application/json", body)
            }
        }
    }

    private fun sendResponse(writer: OutputStreamWriter, code: Int, contentType: String, body: String) {
        val statusText = if (code == 200) "OK" else "Not Found"
        val bytes = body.toByteArray(Charsets.UTF_8)
        writer.write("HTTP/1.1 $code $statusText\r\n")
        writer.write("Content-Type: $contentType; charset=utf-8\r\n")
        writer.write("Content-Length: ${bytes.size}\r\n")
        writer.write("Access-Control-Allow-Origin: *\r\n")
        writer.write("Connection: close\r\n")
        writer.write("\r\n")
        writer.write(body)
        writer.flush()
    }

    companion object {
        const val DEFAULT_PORT = 8787
    }
}
