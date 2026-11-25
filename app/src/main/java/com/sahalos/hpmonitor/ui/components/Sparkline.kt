package com.sahalos.hpmonitor.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Path
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Sparkline rendered from a stride-3 buffer: [avg, min, max] per bucket.
 * Draws a min-max band plus the average line. Auto-scales vertically with
 * a minimum span guard so flat series still render.
 */
@Composable
fun Sparkline(
    data: FloatArray,
    color: Color,
    modifier: Modifier = Modifier.fillMaxWidth().height(56.dp),
) {
    val buckets = data.size / 3
    Canvas(modifier = modifier) {
        if (buckets < 2) return@Canvas
        val w = size.width
        val h = size.height

        var lo = Float.MAX_VALUE
        var hi = Float.MIN_VALUE
        for (i in 0 until buckets) {
            lo = minOf(lo, data[i * 3 + 1])
            hi = maxOf(hi, data[i * 3 + 2])
        }
        var span = hi - lo
        if (span < 1e-6f) span = 1f

        val xStep = w / (buckets - 1)
        val yOf = { value: Float -> h - (value - lo) / span * h }

        // Min-max band
        val band = Path()
        for (i in 0 until buckets) {
            val x = i * xStep
            val y = yOf(data[i * 3 + 2])
            if (i == 0) band.moveTo(x, y) else band.lineTo(x, y)
        }
        for (i in buckets - 1 downTo 0) {
            band.lineTo(i * xStep, yOf(data[i * 3 + 1]))
        }
        band.close()
        drawPath(band, color.copy(alpha = 0.22f))

        // Average line
        val avg = Path()
        for (i in 0 until buckets) {
            val x = i * xStep
            val y = yOf(data[i * 3])
            if (i == 0) avg.moveTo(x, y) else avg.lineTo(x, y)
        }
        drawPath(avg, color, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f))
    }
}
