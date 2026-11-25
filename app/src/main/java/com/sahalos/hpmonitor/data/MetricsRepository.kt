package com.sahalos.hpmonitor.data

import java.io.IOException
import java.net.URL
import java.net.HttpURLConnection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

/**
 * LAN metrics client. One mutex serializes fetches so overlapping loop
 * restarts can never double-post a sample to the history engine.
 * Plain HttpURLConnection: zero extra dependencies, fine for 3 s polling.
 */
class MetricsRepository {

    private val json = Json {
        ignoreUnknownKeys = true // agent schema may drift
        isLenient = true
        coerceInputValues = true
    }

    private val fetchMutex = Mutex()

    suspend fun fetchOnce(url: String): Result<MetricsSnapshot> = withContext(Dispatchers.IO) {
        fetchMutex.withLock {
            runCatching {
                val trimmed = url.trim()
                val clean = trimmed.removeSuffix("/").removeSuffix("/metrics").removeSuffix("/")
                val base = if (
                    clean.startsWith("http://") || clean.startsWith("https://")
                ) clean else "http://$clean"
                val conn = URL("$base/metrics").openConnection() as HttpURLConnection
                conn.connectTimeout = CONNECT_TIMEOUT_MS
                conn.readTimeout = READ_TIMEOUT_MS
                conn.requestMethod = "GET"
                conn.instanceFollowRedirects = true
                try {
                    val code = conn.responseCode
                    if (code != 200) throw IOException("HTTP $code")
                    val body = conn.inputStream.bufferedReader().use { it.readText() }
                    val head = body.trimStart()
                    if (head.startsWith('<')) throw IOException("Respons bukan JSON")
                    json.decodeFromString<MetricsSnapshot>(body)
                } finally {
                    conn.disconnect()
                }
            }
        }
    }

    companion object {
        private const val CONNECT_TIMEOUT_MS = 3_000
        private const val READ_TIMEOUT_MS = 4_000
    }
}
