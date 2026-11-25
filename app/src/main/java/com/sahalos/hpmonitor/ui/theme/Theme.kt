package com.sahalos.hpmonitor.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

@Composable
fun HpMonitorTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            background = MonitorBlack,
            surface = MonitorSurface,
            surfaceVariant = MonitorSurfaceVariant,
            onBackground = TextPrimary,
            onSurface = TextPrimary,
            onSurfaceVariant = TextSecondary,
            primary = MonitorAccent,
            onPrimary = MonitorOnAccent,
            secondary = MonitorAccent,
            onSecondary = MonitorOnAccent,
            outline = MonitorOutline,
        ),
        typography = Typography(),
        content = content,
    )
}
