package com.sahalos.hpmonitor.core

/**
 * Pure-Kotlin fallback engine with identical semantics to the Rust core:
 * fixed-capacity ring buffer, bucketed [avg, min, max] sparklines, and
 * [count, last, avg, min, max] window statistics.
 */
class KotlinHistory : MetricsHistory {

    private data class Sample(val ts: Long, val v: FloatArray)

    private val data = ArrayDeque<Sample>()
    private var capacity = 360

    override fun init(capacity: Int) {
        this.capacity = capacity.coerceAtLeast(2).coerceAtMost(10_000)
        data.clear()
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
        data.addLast(
            Sample(ts, floatArrayOf(cpu, mem, swap, rxBps.toFloat(), txBps.toFloat(), battery, temp))
        )
        while (data.size > capacity) data.removeFirst()
    }

    private fun series(metric: Int): List<Float> {
        if (metric !in 0 until Metric.COUNT) return emptyList()
        return data.map { it.v[metric] }
    }

    override fun sparkline(metric: Int, points: Int): FloatArray {
        val values = series(metric)
        val n = values.size
        if (n == 0 || points <= 0) return FloatArray(0)
        val p = points.coerceAtMost(n)
        val bucket = (n + p - 1) / p
        val out = ArrayList<Float>(n)
        var start = 0
        while (start < n) {
            val end = minOf(start + bucket, n)
            var sum = 0.0f
            var mn = Float.MAX_VALUE
            var mx = Float.MIN_VALUE
            for (i in start until end) {
                val v = values[i]
                sum += v
                if (v < mn) mn = v
                if (v > mx) mx = v
            }
            out.add(sum / (end - start))
            out.add(mn)
            out.add(mx)
            start = end
        }
        return FloatArray(out.size) { out[it] }
    }

    override fun stats(metric: Int): FloatArray {
        val values = series(metric)
        val n = values.size
        if (n == 0) return FloatArray(5)
        var sum = 0.0f
        var mn = Float.MAX_VALUE
        var mx = Float.MIN_VALUE
        for (v in values) {
            sum += v
            if (v < mn) mn = v
            if (v > mx) mx = v
        }
        return floatArrayOf(n.toFloat(), values[n - 1], sum / n, mn, mx)
    }

    override fun clear() {
        data.clear()
    }

    override fun count(): Int = data.size
}
