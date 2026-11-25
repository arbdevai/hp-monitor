package com.sahalos.hpmonitor.core

/**
 * Fail-safe engine selector: uses the Rust core when libmonitor_core.so
 * loads for this ABI, otherwise the pure-Kotlin fallback with identical
 * behavior. The app never crashes because of a missing native library.
 */
object MetricsCoreBridge {

    const val CAPACITY = 360 // ~18 minutes of history at 3 s polling

    val nativeActive: Boolean = run {
        try {
            System.loadLibrary("monitor_core")
            true
        } catch (t: Throwable) {
            false
        }
    }

    val history: MetricsHistory by lazy {
        val h = if (nativeActive) NativeHistory() else KotlinHistory()
        h.init(CAPACITY)
        h
    }
}
