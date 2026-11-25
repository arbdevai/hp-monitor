package com.sahalos.hpmonitor.core

/**
 * Metrics history engine contract.
 *
 * Implemented by [NativeHistory] (Rust, libmonitor_core.so) and by
 * [KotlinHistory] (pure-Kotlin fallback when the .so is unavailable for
 * the device ABI).
 */
interface MetricsHistory {

    fun init(capacity: Int)

    fun push(
        ts: Long,
        cpu: Float,
        mem: Float,
        swap: Float,
        rxBps: Long,
        txBps: Long,
        battery: Float,
        temp: Float,
    )

    /** Flat array with stride 3: [avg, min, max] repeated per bucket. */
    fun sparkline(metric: Int, points: Int): FloatArray

    /** [count, last, avg, min, max]. */
    fun stats(metric: Int): FloatArray

    fun clear()

    fun count(): Int
}
