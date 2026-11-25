package com.sahalos.hpmonitor.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.sahalos.hpmonitor.ui.theme.HpMonitorTheme
import com.sahalos.hpmonitor.ui.theme.MonitorSurfaceVariant

/**
 * Zero-dependency shimmer for skeleton placeholders — replaces full-screen
 * spinners so first paint mirrors the real layout.
 */
fun Modifier.shimmerEffect(): Modifier = composed {
    var measured by remember { mutableStateOf(IntSize.Zero) }
    val transition = rememberInfiniteTransition(label = "shimmer")
    val startOffsetX by transition.animateFloat(
        initialValue = -(2 * measured.width).toFloat(),
        targetValue = (2 * measured.width).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmerOffset",
    )

    background(
        brush = Brush.linearGradient(
            colors = listOf(
                MonitorSurfaceVariant.copy(alpha = 0.55f),
                MonitorSurfaceVariant.copy(alpha = 0.20f),
                MonitorSurfaceVariant.copy(alpha = 0.55f),
            ),
            start = Offset(startOffsetX, 0f),
            end = Offset(startOffsetX + measured.width.toFloat(), measured.height.toFloat()),
        )
    ).onGloballyPositioned { measured = it.size }
}

@Composable
fun SkeletonBlock(
    modifier: Modifier = Modifier,
    width: Dp = 24.dp,
    height: Dp = 12.dp,
    corner: Dp = 6.dp,
) {
    Box(
        modifier = modifier
            .width(width)
            .height(height)
            .shimmerEffect()
    )
}

@Composable
fun SkeletonCard(modifier: Modifier = Modifier, rows: Int = 3) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MonitorSurfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
            .padding(16.dp),
    ) {
        SkeletonBlock(width = 96.dp, height = 14.dp)
        Spacer(Modifier.height(12.dp))
        repeat(rows) {
            SkeletonBlock(modifier = Modifier.fillMaxWidth(), height = 12.dp)
            if (it < rows - 1) Spacer(Modifier.height(8.dp))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SkeletonCardPreview() {
    HpMonitorTheme {
        MaterialTheme(colorScheme = MaterialTheme.colorScheme) {
            SkeletonCard()
        }
    }
}
