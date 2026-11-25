package com.sahalos.hpmonitor.ui.dashboard

import android.app.Application
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sahalos.hpmonitor.BuildConfig
import com.sahalos.hpmonitor.core.Metric
import com.sahalos.hpmonitor.core.MetricsCoreBridge
import com.sahalos.hpmonitor.data.DeviceTarget
import com.sahalos.hpmonitor.data.MetricsRepository
import com.sahalos.hpmonitor.data.MetricsSnapshot
import com.sahalos.hpmonitor.data.Preferences
import com.sahalos.hpmonitor.data.UpdateManager
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class UpdateUi(
    val state: String, // idle | checking | available | downloading | ready | error
    val progress: Float = 0f,
    val message: String = "",
)

data class DashboardState(
    val snapshot: MetricsSnapshot? = null,
    val connected: Boolean = false,
    val lastError: String? = null,
    val lastUpdated: Long = 0L,
    val latencyMs: Long? = null,
    val sparks: Map<Int, FloatArray> = emptyMap(),
    val stats: Map<Int, FloatArray> = emptyMap(),
    val serverUrl: String = Preferences.DEFAULT_URL,
    val savedDevices: List<DeviceTarget> = emptyList(),
    val update: UpdateUi = UpdateUi(state = "idle"),
    val updateInfo: UpdateManager.UpdateInfo? = null,
    val currentVersion: String = BuildConfig.VERSION_NAME,
    val compactMode: Boolean = false,
    val hudDeskMode: Boolean = false,
    val fullscreenMode: Boolean = false,
    val keepScreenOn: Boolean = true,
)

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MetricsRepository()
    private val history = MetricsCoreBridge.history
    private val preferences = Preferences(application)
    val updateManager = UpdateManager(application)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var pendingApk: File? = null

    private val _state = MutableStateFlow(
        DashboardState(
            serverUrl = preferences.serverUrl,
            savedDevices = preferences.getSavedDevices(),
            currentVersion = BuildConfig.VERSION_NAME,
            compactMode = preferences.compactMode,
            hudDeskMode = preferences.hudDeskMode,
            fullscreenMode = preferences.fullscreenMode,
            keepScreenOn = preferences.keepScreenOn,
        )
    )
    val state: StateFlow<DashboardState> = _state

    /**
     * Polling loop. Must be invoked under repeatOnLifecycle so the loop
     * cancels (and battery polling stops) when the activity is not started.
     */
    suspend fun runLoop() {
        while (currentCoroutineContext().isActive) {
            val url = preferences.serverUrl
            val t0 = SystemClock.elapsedRealtime()
            repository.fetchOnce(url)
                .onSuccess { snap ->
                    val latency = (SystemClock.elapsedRealtime() - t0).coerceAtLeast(1L)
                    history.push(
                        ts = snap.ts,
                        cpu = snap.cpu.overall_pct ?: 0f,
                        mem = snap.mem.used_pct ?: 0f,
                        swap = snap.mem.swap_used_pct ?: 0f,
                        rxBps = snap.net.rx_rate_bps ?: 0L,
                        txBps = snap.net.tx_rate_bps ?: 0L,
                        battery = snap.battery.capacity_pct?.toFloat() ?: 0f,
                        temp = snap.thermal.max_c ?: snap.battery.temperature_c ?: 0f,
                    )
                    _state.update { st ->
                        st.copy(
                            snapshot = snap,
                            connected = true,
                            lastError = null,
                            lastUpdated = System.currentTimeMillis(),
                            latencyMs = latency,
                            sparks = (0 until Metric.COUNT).associateWith { m ->
                                history.sparkline(m, SPARKLINE_POINTS)
                            },
                            stats = (0 until Metric.COUNT).associateWith { m ->
                                history.stats(m)
                            },
                        )
                    }
                }
                .onFailure { e ->
                    _state.update { st ->
                        st.copy(
                            connected = false,
                            lastError = e.message ?: "Gagal terhubung ke server",
                            latencyMs = null,
                        )
                    }
                }
            delay(POLL_INTERVAL_MS)
        }
    }

    fun applyUrl(url: String) {
        val normalized = url.trim().ifEmpty { Preferences.DEFAULT_URL }
        preferences.serverUrl = normalized
        _state.update {
            it.copy(
                serverUrl = normalized,
                snapshot = null,
                connected = false,
                latencyMs = null,
            )
        }
    }

    fun switchDevice(device: DeviceTarget) {
        applyUrl(device.url)
    }

    fun addDevice(name: String, url: String) {
        preferences.addOrUpdateDevice(name, url)
        _state.update {
            it.copy(
                savedDevices = preferences.getSavedDevices(),
                serverUrl = url.trim(),
                snapshot = null,
                connected = false,
                latencyMs = null,
            )
        }
    }

    fun removeDevice(url: String) {
        preferences.removeDevice(url)
        _state.update {
            it.copy(savedDevices = preferences.getSavedDevices())
        }
    }

    fun sendRemoteControl(action: String) {
        scope.launch {
            repository.sendControl(_state.value.serverUrl, action)
        }
    }

    fun toggleCompactMode() {
        val newMode = !_state.value.compactMode
        preferences.compactMode = newMode
        _state.update { it.copy(compactMode = newMode) }
    }

    fun toggleHudDeskMode() {
        val newMode = !_state.value.hudDeskMode
        preferences.hudDeskMode = newMode
        _state.update { it.copy(hudDeskMode = newMode) }
    }

    fun toggleFullscreenMode() {
        val newMode = !_state.value.fullscreenMode
        preferences.fullscreenMode = newMode
        _state.update { it.copy(fullscreenMode = newMode) }
    }

    fun toggleKeepScreenOn() {
        val newMode = !_state.value.keepScreenOn
        preferences.keepScreenOn = newMode
        _state.update { it.copy(keepScreenOn = newMode) }
    }

    fun checkForUpdate() {
        scope.launch {
            _state.update {
                it.copy(
                    update = UpdateUi(
                        state = "checking",
                        message = "Memeriksa rilis terbaru...",
                    )
                )
            }
            updateManager.checkLatest(BuildConfig.VERSION_NAME)
                .onSuccess { info ->
                    _state.update { st ->
                        st.copy(
                            updateInfo = info,
                            update = if (info == null) {
                                UpdateUi(state = "idle", message = "Sudah versi terbaru")
                            } else {
                                UpdateUi(
                                    state = "available",
                                    message = "Tersedia versi ${info.tag} (${info.name})",
                                )
                            },
                        )
                    }
                }
                .onFailure { e ->
                    _state.update { st ->
                        st.copy(
                            update = st.update.copy(
                                state = "error",
                                message = e.message ?: "Gagal memeriksa pembaruan",
                            )
                        )
                    }
                }
        }
    }

    fun downloadUpdate() {
        val info = _state.value.updateInfo ?: return
        scope.launch {
            _state.update {
                it.copy(update = it.update.copy(state = "downloading", progress = 0f))
            }
            updateManager.download(info) { p ->
                _state.update { st ->
                    st.copy(update = st.update.copy(progress = p.fraction.coerceIn(0f, 1f)))
                }
            }.onSuccess { file ->
                pendingApk = file
                _state.update { st ->
                    st.copy(
                        update = st.update.copy(
                            state = "ready",
                            progress = 1f,
                            message = "APK terverifikasi. Siap dipasang.",
                        )
                    )
                }
            }.onFailure { e ->
                _state.update { st ->
                    st.copy(
                        update = st.update.copy(
                            state = "error",
                            message = e.message ?: "Unduhan gagal",
                        )
                    )
                }
            }
        }
    }

    fun installPendingUpdate() {
        val file = pendingApk ?: run {
            _state.update {
                it.copy(update = it.update.copy(state = "error", message = "File APK belum siap dipasang"))
            }
            return
        }
        updateManager.installApk(file).onFailure { e ->
            _state.update { st ->
                st.copy(update = st.update.copy(state = "error", message = e.message ?: "Gagal membuka installer"))
            }
        }
    }

    override fun onCleared() {
        scope.cancel()
        history.clear()
        super.onCleared()
    }

    companion object {
        private const val POLL_INTERVAL_MS = 3_000L
        private const val SPARKLINE_POINTS = 48
    }
}
