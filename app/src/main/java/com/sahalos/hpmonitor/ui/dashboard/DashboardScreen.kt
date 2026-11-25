package com.sahalos.hpmonitor.ui.dashboard

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.DeveloperBoard
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material.icons.filled.ViewCompact
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sahalos.hpmonitor.core.Metric
import com.sahalos.hpmonitor.data.MetricsSnapshot
import com.sahalos.hpmonitor.ui.components.MetricCard
import com.sahalos.hpmonitor.ui.components.SkeletonCard
import com.sahalos.hpmonitor.ui.components.Sparkline
import com.sahalos.hpmonitor.ui.theme.MonitorAccent
import com.sahalos.hpmonitor.ui.theme.MonitorBlack
import com.sahalos.hpmonitor.ui.theme.MonitorBorder
import com.sahalos.hpmonitor.ui.theme.MonitorCard
import com.sahalos.hpmonitor.ui.theme.MonitorSurfaceVariant
import com.sahalos.hpmonitor.ui.theme.StatusCrit
import com.sahalos.hpmonitor.ui.theme.StatusGood
import com.sahalos.hpmonitor.ui.theme.StatusWarn
import com.sahalos.hpmonitor.ui.theme.TextMuted
import com.sahalos.hpmonitor.ui.theme.TextPrimary
import com.sahalos.hpmonitor.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    vm: DashboardViewModel,
    onOpenSettings: () -> Unit,
    onOpenHost: () -> Unit,
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val snapshot = state.snapshot

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val isHudMode = state.hudDeskMode
    val isFullscreen = state.fullscreenMode || isHudMode

    if (isHudMode) {
        DeskHudView(
            state = state,
            isLandscape = isLandscape,
            onToggleHud = { vm.toggleHudDeskMode() },
            onToggleFullscreen = { vm.toggleFullscreenMode() },
            onOpenSettings = onOpenSettings,
            onOpenHost = onOpenHost,
        )
        return
    }

    val compact = state.compactMode || isLandscape
    val contentPadding = if (compact) 6.dp else 14.dp
    val itemSpacing = if (compact) 6.dp else 10.dp

    Scaffold(
        containerColor = MonitorBlack,
        topBar = {
            if (isFullscreen) {
                TelemetryHeader(
                    state = state,
                    onToggleHud = { vm.toggleHudDeskMode() },
                    onToggleFullscreen = { vm.toggleFullscreenMode() },
                    onOpenHost = onOpenHost,
                    onOpenSettings = onOpenSettings,
                )
            } else {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "HP Monitor",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = if (compact) 16.sp else 18.sp,
                            )
                            Spacer(Modifier.width(8.dp))
                            StatusChip(
                                text = if (state.connected) "ONLINE" else "OFFLINE",
                                color = if (state.connected) StatusGood else StatusCrit,
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MonitorBlack),
                    actions = {
                        IconButton(onClick = { vm.toggleHudDeskMode() }) {
                            Icon(
                                imageVector = Icons.Default.Speed,
                                contentDescription = "Mode Desk HUD",
                                tint = if (state.hudDeskMode) MonitorAccent else TextSecondary,
                            )
                        }
                        IconButton(onClick = { vm.toggleFullscreenMode() }) {
                            Icon(
                                imageVector = if (state.fullscreenMode) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                                contentDescription = "Sembunyikan App Bar",
                                tint = if (state.fullscreenMode) MonitorAccent else TextSecondary,
                            )
                        }
                        IconButton(onClick = { vm.toggleCompactMode() }) {
                            Icon(
                                imageVector = if (compact) Icons.Default.ViewCompact else Icons.Default.ViewAgenda,
                                contentDescription = if (compact) "Tampilan Normal" else "Tampilan Ringkas",
                                tint = if (state.compactMode) MonitorAccent else TextSecondary,
                            )
                        }
                        IconButton(onClick = onOpenHost) {
                            Icon(
                                imageVector = Icons.Default.Dns,
                                contentDescription = "Mode Host (Target)",
                                tint = TextSecondary,
                            )
                        }
                        IconButton(onClick = onOpenSettings) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Pengaturan",
                                tint = TextSecondary,
                            )
                        }
                    },
                )
            }
        },
    ) { innerPadding ->
        if (snapshot == null && !state.connected) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(contentPadding)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(itemSpacing),
            ) {
                repeat(4) { SkeletonCard(rows = 3) }
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = contentPadding)
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(itemSpacing),
        ) {
            if (!state.connected) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MonitorSurfaceVariant, RoundedCornerShape(8.dp))
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Default.WifiOff,
                        contentDescription = null,
                        tint = StatusCrit,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(
                            "Server tidak terjangkau",
                            color = StatusCrit,
                            style = MaterialTheme.typography.labelMedium,
                        )
                        Text(
                            state.lastError ?: "Periksa alamat server di Pengaturan",
                            color = TextSecondary,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }

            snapshot?.let { sn ->
                if (!isFullscreen) {
                    HeaderRow(sn, state.lastUpdated, compact)
                }

                if (isLandscape) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(itemSpacing),
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(itemSpacing),
                        ) {
                            CpuCard(sn, state.sparks[Metric.CPU] ?: FloatArray(0), compact)
                            BatteryCard(sn, state.sparks[Metric.BATTERY] ?: FloatArray(0), compact)
                        }
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(itemSpacing),
                        ) {
                            MemoryCard(sn, state.sparks[Metric.MEM] ?: FloatArray(0), compact)
                            NetworkCard(sn, state.sparks, compact)
                        }
                    }
                    ProcessesCard(sn, compact)
                } else {
                    CpuCard(sn, state.sparks[Metric.CPU] ?: FloatArray(0), compact)
                    MemoryCard(sn, state.sparks[Metric.MEM] ?: FloatArray(0), compact)
                    BatteryCard(sn, state.sparks[Metric.BATTERY] ?: FloatArray(0), compact)
                    NetworkCard(sn, state.sparks, compact)
                    ProcessesCard(sn, compact)
                }
            }

            Text(
                text = if (com.sahalos.hpmonitor.core.MetricsCoreBridge.nativeActive) {
                    "Engine: Rust (libmonitor_core)"
                } else {
                    "Engine: Kotlin (fallback native)"
                },
                color = TextMuted,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
            )
        }
    }
}

/**
 * Dedicated Zero-Scroll Desk Monitor HUD View.
 * Fits 100% of telemetry in screen without any scrolling.
 */
@Composable
private fun DeskHudView(
    state: DashboardState,
    isLandscape: Boolean,
    onToggleHud: () -> Unit,
    onToggleFullscreen: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenHost: () -> Unit,
) {
    val snapshot = state.snapshot

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MonitorBlack)
            .padding(horizontal = 6.dp, vertical = 4.dp),
    ) {
        TelemetryHeader(
            state = state,
            onToggleHud = onToggleHud,
            onToggleFullscreen = onToggleFullscreen,
            onOpenHost = onOpenHost,
            onOpenSettings = onOpenSettings,
        )

        Spacer(Modifier.height(4.dp))

        if (snapshot == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MonitorCard, RoundedCornerShape(6.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "DESK HUD MODE",
                        color = MonitorAccent,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        if (state.connected) "Menunggu data telemetri..." else "Menghubungkan ke ${state.serverUrl}...",
                        color = TextSecondary,
                        fontSize = 11.sp,
                    )
                }
            }
            return
        }

        if (isLandscape) {
            // 3-Column Zero-Scroll Grid for Landscape
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                // Column 1: CPU & TOP PROCS
                HudCpuPanel(
                    sn = snapshot,
                    spark = state.sparks[Metric.CPU] ?: FloatArray(0),
                    modifier = Modifier
                        .weight(1.1f)
                        .fillMaxHeight(),
                )

                // Column 2: MEMORY, SWAP & THERMAL
                HudMemoryPanel(
                    sn = snapshot,
                    spark = state.sparks[Metric.MEM] ?: FloatArray(0),
                    modifier = Modifier
                        .weight(1.0f)
                        .fillMaxHeight(),
                )

                // Column 3: NETWORK & POWER
                HudNetPowerPanel(
                    sn = snapshot,
                    sparks = state.sparks,
                    modifier = Modifier
                        .weight(1.0f)
                        .fillMaxHeight(),
                )
            }
        } else {
            // 3-Row Zero-Scroll Layout for Portrait
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                HudCpuPanel(
                    sn = snapshot,
                    spark = state.sparks[Metric.CPU] ?: FloatArray(0),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                )
                HudMemoryPanel(
                    sn = snapshot,
                    spark = state.sparks[Metric.MEM] ?: FloatArray(0),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                )
                HudNetPowerPanel(
                    sn = snapshot,
                    sparks = state.sparks,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                )
            }
        }
    }
}

@Composable
private fun TelemetryHeader(
    state: DashboardState,
    onToggleHud: () -> Unit,
    onToggleFullscreen: () -> Unit,
    onOpenHost: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val sn = state.snapshot
    val now = if (state.lastUpdated > 0) state.lastUpdated else System.currentTimeMillis()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(28.dp)
            .background(MonitorBlack),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Status indicator dot
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(if (state.connected) StatusGood else StatusCrit),
        )
        Spacer(Modifier.width(6.dp))

        // Digital clock in monospace
        Text(
            text = fmtClock(now),
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            color = MonitorAccent,
        )

        Spacer(Modifier.width(8.dp))

        // Host name & Uptime
        Text(
            text = (sn?.hostname?.ifEmpty { "Target" } ?: "Target")
                .plus(sn?.uptime_s?.let { " • up ${fmtUptime(it)}" } ?: ""),
            fontSize = 11.sp,
            color = TextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )

        // Actions
        IconButton(
            onClick = onToggleHud,
            modifier = Modifier.size(26.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Speed,
                contentDescription = "HUD Mode",
                tint = if (state.hudDeskMode) MonitorAccent else TextSecondary,
                modifier = Modifier.size(16.dp),
            )
        }

        IconButton(
            onClick = onToggleFullscreen,
            modifier = Modifier.size(26.dp),
        ) {
            Icon(
                imageVector = if (state.fullscreenMode) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                contentDescription = "Layar Penuh",
                tint = if (state.fullscreenMode) MonitorAccent else TextSecondary,
                modifier = Modifier.size(16.dp),
            )
        }

        IconButton(
            onClick = onOpenHost,
            modifier = Modifier.size(26.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Dns,
                contentDescription = "Host Agent",
                tint = TextSecondary,
                modifier = Modifier.size(16.dp),
            )
        }

        IconButton(
            onClick = onOpenSettings,
            modifier = Modifier.size(26.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Pengaturan",
                tint = TextSecondary,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

@Composable
private fun HudCpuPanel(
    sn: MetricsSnapshot,
    spark: FloatArray,
    modifier: Modifier = Modifier,
) {
    MetricCard(
        title = "CPU",
        icon = Icons.Default.DeveloperBoard,
        compact = true,
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = sn.cpu.overall_pct?.let { String.format(Locale.US, "%.0f", it) } ?: "--",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
            )
            Text(
                text = "%",
                fontSize = 12.sp,
                color = MonitorAccent,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.weight(1f))
            val load = sn.cpu.load
            if (load.size >= 3) {
                Text(
                    text = String.format(Locale.US, "%.1f/%.1f", load[0], load[1]),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    color = TextSecondary,
                )
            }
        }

        Spacer(Modifier.height(4.dp))

        // Mini per-core bars
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(14.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            val cores = sn.cpu.per_core_pct
            for (pct in cores) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .background(MonitorSurfaceVariant, RoundedCornerShape(1.dp)),
                ) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .fillMaxHeight((pct / 100f).coerceIn(0f, 1f))
                            .background(MonitorAccent),
                    )
                }
            }
        }

        Spacer(Modifier.height(4.dp))

        Sparkline(
            data = spark,
            color = MonitorAccent,
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp),
        )

        Spacer(Modifier.height(4.dp))

        // Top 2 processes in monospace
        sn.procs.take(2).forEach { p ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = p.name,
                    fontSize = 10.sp,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = String.format(Locale.US, "%.0f%%", p.cpu_pct),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    color = TextPrimary,
                )
            }
        }
    }
}

@Composable
private fun HudMemoryPanel(
    sn: MetricsSnapshot,
    spark: FloatArray,
    modifier: Modifier = Modifier,
) {
    val mem = sn.mem
    MetricCard(
        title = "RAM & SWAP",
        icon = Icons.Default.Memory,
        compact = true,
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "${fmtKb(mem.used_kb)} / ${fmtKb(mem.total_kb)}",
                fontSize = 11.sp,
                color = TextPrimary,
                fontWeight = FontWeight.Medium,
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = mem.used_pct?.let { String.format(Locale.US, "%.0f%%", it) } ?: "--",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MonitorAccent,
            )
        }

        Spacer(Modifier.height(3.dp))

        LinearProgressIndicator(
            progress = { (mem.used_pct ?: 0f) / 100f },
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp),
            color = MonitorAccent,
            trackColor = MonitorSurfaceVariant,
        )

        Spacer(Modifier.height(4.dp))

        Sparkline(
            data = spark,
            color = MonitorAccent,
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp),
        )

        Spacer(Modifier.height(4.dp))

        if (mem.swap_total_kb > 0) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Swap ${fmtKb(mem.swap_used_kb)}",
                    fontSize = 10.sp,
                    color = TextSecondary,
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = mem.swap_used_pct?.let { String.format(Locale.US, "%.0f%%", it) } ?: "0%",
                    fontSize = 10.sp,
                    color = TextSecondary,
                )
            }
            Spacer(Modifier.height(2.dp))
            LinearProgressIndicator(
                progress = { (mem.swap_used_pct ?: 0f) / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp),
                color = MonitorAccent.copy(alpha = 0.6f),
                trackColor = MonitorSurfaceVariant,
            )
        }

        val temp = sn.thermal.max_c ?: sn.battery.temperature_c
        if (temp != null) {
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Thermostat,
                    contentDescription = null,
                    tint = if (temp > 65f) StatusCrit else if (temp > 50f) StatusWarn else StatusGood,
                    modifier = Modifier.size(12.dp),
                )
                Spacer(Modifier.width(3.dp))
                Text(
                    text = String.format(Locale.US, "Chip %.1f°C", temp),
                    fontSize = 10.sp,
                    color = TextSecondary,
                )
            }
        }
    }
}

@Composable
private fun HudNetPowerPanel(
    sn: MetricsSnapshot,
    sparks: Map<Int, FloatArray>,
    modifier: Modifier = Modifier,
) {
    val net = sn.net
    val bat = sn.battery

    MetricCard(
        title = "NET & POWER",
        icon = Icons.Default.Speed,
        compact = true,
        modifier = modifier,
    ) {
        // Network rates
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "↓ ${fmtRate(net.rx_rate_bps)}",
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = MonitorAccent,
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = "↑ ${fmtRate(net.tx_rate_bps)}",
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = StatusGood,
            )
        }

        Spacer(Modifier.height(4.dp))

        Sparkline(
            data = sparks[Metric.RX] ?: FloatArray(0),
            color = MonitorAccent,
            modifier = Modifier
                .fillMaxWidth()
                .height(22.dp),
        )

        Spacer(Modifier.height(4.dp))

        // Battery row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Default.BatteryFull,
                contentDescription = null,
                tint = if ((bat.capacity_pct ?: 100) < 20) StatusCrit else StatusGood,
                modifier = Modifier.size(13.dp),
            )
            Spacer(Modifier.width(3.dp))
            Text(
                text = "${bat.capacity_pct ?: "--"}%",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = bat.status ?: "",
                fontSize = 10.sp,
                color = TextSecondary,
                maxLines = 1,
            )
            Spacer(Modifier.weight(1f))
            bat.temperature_c?.let {
                Text(
                    text = String.format(Locale.US, "%.1f°C", it),
                    fontSize = 10.sp,
                    color = TextSecondary,
                )
            }
        }

        Spacer(Modifier.height(3.dp))

        Sparkline(
            data = sparks[Metric.BATTERY] ?: FloatArray(0),
            color = StatusGood,
            modifier = Modifier
                .fillMaxWidth()
                .height(22.dp),
        )
    }
}

@Composable
private fun HeaderRow(sn: MetricsSnapshot, lastUpdated: Long, compact: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = sn.hostname.ifEmpty { "Device target" },
                style = if (compact) MaterialTheme.typography.titleSmall else MaterialTheme.typography.titleMedium,
                color = TextPrimary,
            )
            Text(
                text = "Uptime ${fmtUptime(sn.uptime_s)}",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
            )
        }
        if (lastUpdated > 0) {
            Text(
                text = fmtClock(lastUpdated),
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
            )
        }
    }
}

@Composable
private fun CpuCard(sn: MetricsSnapshot, spark: FloatArray, compact: Boolean = false) {
    MetricCard(title = "CPU", icon = Icons.Default.DeveloperBoard, compact = compact) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = sn.cpu.overall_pct?.let { String.format(Locale.US, "%.0f", it) } ?: "--",
                fontSize = if (compact) 24.sp else 32.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
            )
            Text(" %", fontSize = if (compact) 13.sp else 16.sp, color = MonitorAccent)
            Spacer(Modifier.weight(1f))
            val load = sn.cpu.load
            if (load.size >= 3) {
                Text(
                    text = String.format(
                        Locale.US,
                        "Load %.1f/%.1f/%.1f",
                        load[0],
                        load[1],
                        load[2],
                    ),
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                )
            }
        }
        Spacer(Modifier.height(if (compact) 4.dp else 8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (compact) 16.dp else 22.dp),
            horizontalArrangement = Arrangement.spacedBy(if (compact) 2.dp else 3.dp),
        ) {
            val cores = sn.cpu.per_core_pct
            for (pct in cores) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .background(MonitorSurfaceVariant, RoundedCornerShape(2.dp)),
                ) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .fillMaxHeight((pct / 100f).coerceIn(0f, 1f))
                            .background(MonitorAccent),
                    )
                }
            }
        }
        Spacer(Modifier.height(if (compact) 4.dp else 8.dp))
        Sparkline(
            data = spark,
            color = MonitorAccent,
            modifier = Modifier
                .fillMaxWidth()
                .height(if (compact) 30.dp else 50.dp),
        )
    }
}

@Composable
private fun MemoryCard(sn: MetricsSnapshot, spark: FloatArray, compact: Boolean = false) {
    val mem = sn.mem
    MetricCard(title = "Memori", icon = Icons.Default.Memory, compact = compact) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "${fmtKb(mem.used_kb)} / ${fmtKb(mem.total_kb)}",
                style = if (compact) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.titleMedium,
                color = TextPrimary,
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = mem.used_pct?.let { String.format(Locale.US, "%.0f%%", it) } ?: "--",
                style = if (compact) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.titleMedium,
                color = MonitorAccent,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { (mem.used_pct ?: 0f) / 100f },
            modifier = Modifier
                .fillMaxWidth()
                .height(if (compact) 4.dp else 6.dp),
            color = MonitorAccent,
            trackColor = MonitorSurfaceVariant,
        )
        if (mem.swap_total_kb > 0) {
            Spacer(Modifier.height(if (compact) 4.dp else 10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Storage,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(if (compact) 13.dp else 15.dp),
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "Swap ${fmtKb(mem.swap_used_kb)} / ${fmtKb(mem.swap_total_kb)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = mem.swap_used_pct?.let { String.format(Locale.US, "%.0f%%", it) } ?: "--",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                )
            }
            Spacer(Modifier.height(3.dp))
            LinearProgressIndicator(
                progress = { (mem.swap_used_pct ?: 0f) / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (compact) 3.dp else 4.dp),
                color = MonitorAccent.copy(alpha = 0.6f),
                trackColor = MonitorSurfaceVariant,
            )
        }
        Spacer(Modifier.height(if (compact) 4.dp else 10.dp))
        Sparkline(
            data = spark,
            color = MonitorAccent,
            modifier = Modifier
                .fillMaxWidth()
                .height(if (compact) 30.dp else 50.dp),
        )
    }
}

@Composable
private fun BatteryCard(sn: MetricsSnapshot, spark: FloatArray, compact: Boolean = false) {
    val b = sn.battery
    MetricCard(title = "Baterai & Termal", icon = Icons.Default.BatteryFull, compact = compact) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = b.capacity_pct?.toString()?.plus("%") ?: "--",
                fontSize = if (compact) 24.sp else 32.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
            )
            Spacer(Modifier.width(if (compact) 8.dp else 12.dp))
            Column {
                Text(
                    text = b.status ?: "Tidak diketahui",
                    style = if (compact) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium,
                    color = TextPrimary,
                )
                val v = b.voltage_mv?.toLongOrNull()
                Text(
                    text = (b.temperature_c?.let { String.format(Locale.US, "%.1f C", it) } ?: "-- C")
                        .plus(if (v != null) " | ${String.format(Locale.US, "%.2f V", v / 1000.0)}" else ""),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                )
            }
        }
        if (sn.thermal.max_c != null) {
            Spacer(Modifier.height(if (compact) 4.dp else 8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Thermostat,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(if (compact) 13.dp else 15.dp),
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = String.format(
                        Locale.US,
                        "Chip: %.1f C (%s)",
                        sn.thermal.max_c,
                        sn.thermal.zone ?: "-",
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                )
            }
        }
        Spacer(Modifier.height(if (compact) 4.dp else 8.dp))
        Sparkline(
            data = spark,
            color = StatusGood,
            modifier = Modifier
                .fillMaxWidth()
                .height(if (compact) 30.dp else 50.dp),
        )
    }
}

@Composable
private fun NetworkCard(sn: MetricsSnapshot, sparks: Map<Int, FloatArray>, compact: Boolean = false) {
    val n = sn.net
    MetricCard(title = "Jaringan (wlan0)", icon = Icons.Default.Speed, compact = compact) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Down", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                Text(
                    text = fmtRate(n.rx_rate_bps),
                    fontFamily = FontFamily.Monospace,
                    style = if (compact) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.titleMedium,
                    color = MonitorAccent,
                )
            }
            Column(Modifier.weight(1f)) {
                Text("Up", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                Text(
                    text = fmtRate(n.tx_rate_bps),
                    fontFamily = FontFamily.Monospace,
                    style = if (compact) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.titleMedium,
                    color = StatusGood,
                )
            }
        }
        Spacer(Modifier.height(if (compact) 4.dp else 8.dp))
        Sparkline(
            data = sparks[Metric.RX] ?: FloatArray(0),
            color = MonitorAccent,
            modifier = Modifier
                .fillMaxWidth()
                .height(if (compact) 24.dp else 40.dp),
        )
        Spacer(Modifier.height(3.dp))
        Sparkline(
            data = sparks[Metric.TX] ?: FloatArray(0),
            color = StatusGood,
            modifier = Modifier
                .fillMaxWidth()
                .height(if (compact) 24.dp else 40.dp),
        )
    }
}

@Composable
private fun ProcessesCard(sn: MetricsSnapshot, compact: Boolean = false) {
    MetricCard(title = "Proses Teratas (CPU)", icon = Icons.Default.DeveloperBoard, compact = compact) {
        sn.procs.forEachIndexed { index, p ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (compact) 22.dp else 30.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "${index + 1}",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    modifier = Modifier.width(if (compact) 14.dp else 18.dp),
                )
                Text(
                    text = p.name,
                    style = if (compact) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = String.format(Locale.US, "%.0f%%", p.cpu_pct),
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    modifier = Modifier.width(if (compact) 40.dp else 50.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.End,
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = String.format(Locale.US, "%.0f%%", p.mem_pct),
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    modifier = Modifier.width(if (compact) 36.dp else 44.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.End,
                )
            }
            if (index < sn.procs.size - 1) {
                Spacer(Modifier.height(1.dp))
            }
        }
        if (sn.procs.isEmpty()) {
            Text(
                "Tidak ada data proses",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
            )
        }
    }
}

@Composable
private fun StatusChip(text: String, color: Color) {
    Box(
        modifier = Modifier
            .background(color.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        Text(
            text = text,
            color = color,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
        )
    }
}

private fun fmtClock(epochMs: Long): String =
    SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(epochMs))

private fun fmtUptime(seconds: Long): String {
    val d = seconds / 86400
    val h = (seconds % 86400) / 3600
    val m = (seconds % 3600) / 60
    return if (d > 0) "${d}d ${h}h" else if (h > 0) "${h}h ${m}m" else "${m}m"
}

private fun fmtKb(kb: Long): String =
    if (kb >= 1024 * 1024) String.format(Locale.US, "%.1f GB", kb / (1024f * 1024f))
    else String.format(Locale.US, "%.0f MB", kb / 1024f)

private fun fmtRate(bps: Long?): String {
    if (bps == null || bps < 0) return "--"
    return if (bps >= 1024 * 1024) String.format(Locale.US, "%.1f MB/s", bps / (1024f * 1024f))
    else if (bps >= 1024) String.format(Locale.US, "%.0f KB/s", bps / 1024f)
    else "$bps B/s"
}
