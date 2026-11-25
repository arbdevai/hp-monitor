package com.sahalos.hpmonitor.core

import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * JNI binding to the Rust monitor-core engine.
 *
 * Float results arrive as little-endian byte arrays — a stable,
 * version-proof ABI that avoids typed-array construction edge cases.
 */
class NativeHistory : MetricsHistory {

    external fun nativeInit(capacity: Int): Boolean
    external fun nativePush(
        ts: Long,
        cpu: Float,
        mem: Float,
        swap: Float,
        rxBps: Long,
        txBps: Long,
        battery: Float,
        temp: Float
    )
    external fun nativeSparkline(metric: Int, points: Int): ByteArray?
    external fun nativeStats(metric: Int): ByteArray?
    external fun nativeClear()
    external fun nativeCount(): Int

    private fun toFloats(raw: ByteArray?): FloatArray {
        if (raw == null || raw.size < 4) return FloatArray(0)
        val n = raw.size / 4
        val buf = ByteBuffer.wrap(raw, 0, n * 4).order(ByteOrder.LITTLE_ENDIAN)
        val out = FloatArray(n)
        for (i in 0 until n) out[i] = buf.float
        return out
    }

    override fun init(capacity: Int) {
        nativeInit(capacity)
    }

    override fun push(
        ts: Long,
        cpu: Float,
        mem: Float,
        swap: Float,
        rxBps: Long,
        txBps: Long,
        battery: Float,
        temp: Float
    ) {
        nativePush(ts, cpu, mem, swap, rxBps, txBps, battery, temp)
    }

    override fun sparkline(metric: Int, points: Int): FloatArray =
        toFloats(nativeSparkline(metric, points))

    override fun stats(metric: Int): FloatArray {
        val s = toFloats(nativeStats(metric))
        return if (s.size == 5) s else FloatArray(5)
    }

    override fun clear() {
        nativeClear()
    }

    override fun count(): Int = nativeCount()
}
