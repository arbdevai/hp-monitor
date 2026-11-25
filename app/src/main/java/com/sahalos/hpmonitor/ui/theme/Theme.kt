package com.sahalos.hpmonitor.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun HpMonitorTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            background = MonitorBlack,
            surface = MonitorCard,
            surfaceVariant = MonitorSurfaceVariant,
            surfaceTint = Color.Transparent, // Eliminates M3 tonal reddish/purple tinting
            onBackground = TextPrimary,
            onSurface = TextPrimary,
            onSurfaceVariant = TextSecondary,
            primary = MonitorAccent,
            onPrimary = MonitorOnAccent,
            secondary = MonitorAccent,
            onSecondary = MonitorOnAccent,
            outline = MonitorBorder,
            outlineVariant = MonitorBorder,
        ),
        typography = Typography(),
        content = content,
    )
}
