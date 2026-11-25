package com.sahalos.hpmonitor.ui.dashboard

import android.content.res.Configuration
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.DeveloperBoard
import androidx.compose.material.icons.filled.Dns
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
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
import com.sahalos.hpmonitor.ui.theme.MonitorSurface
import com.sahalos.hpmonitor.ui.theme.MonitorSurfaceVariant
import com.sahalos.hpmonitor.ui.theme.StatusCrit
import com.sahalos.hpmonitor.ui.theme.StatusGood
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
    val compact = state.compactMode || isLandscape
    val contentPadding = if (compact) 8.dp else 16.dp
    val itemSpacing = if (compact) 8.dp else 12.dp

    Scaffold(
        containerColor = MonitorBlack,
        topBar = {
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
                        .background(MonitorSurfaceVariant, RoundedCornerShape(10.dp))
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Default.WifiOff,
                        contentDescription = null,
                        tint = StatusCrit,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            "Server tidak terjangkau",
                            color = StatusCrit,
                            style = MaterialTheme.typography.labelLarge,
                        )
                        Text(
                            state.lastError ?: "Periksa alamat server di Pengaturan",
                            color = TextSecondary,
                            style = MaterialTheme.typography.bodySmall,
                        )
                        if (state.lastUpdated > 0) {
                            Text(
                                "Data terakhir ${fmtClock(state.lastUpdated)}",
                                color = TextSecondary,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
            }

            snapshot?.let { sn ->
                HeaderRow(sn, state.lastUpdated, compact)

                if (isLandscape) {
                    // Two-column layout in landscape for small screens like J1 Ace (800x480)
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
                color = TextSecondary,
                style = MaterialTheme.typography.labelSmall,
            )
        }
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
                fontSize = if (compact) 26.sp else 34.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
            )
            Text(" %", fontSize = if (compact) 14.sp else 18.sp, color = TextSecondary)
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
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                )
            }
        }
        Spacer(Modifier.height(if (compact) 6.dp else 10.dp))
        // Per-core mini bars
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (compact) 18.dp else 26.dp),
            horizontalArrangement = Arrangement.spacedBy(if (compact) 2.dp else 4.dp),
        ) {
            val cores = sn.cpu.per_core_pct
            for ((i, pct) in cores.withIndex()) {
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
                            .fillMaxHeight(pct / 100f)
                            .background(MonitorAccent.copy(alpha = 0.85f)),
                    )
                }
            }
        }
        Spacer(Modifier.height(if (compact) 6.dp else 10.dp))
        Sparkline(
            data = spark,
            color = MonitorAccent,
            modifier = Modifier.fillMaxWidth().height(if (compact) 36.dp else 56.dp),
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
                color = TextPrimary,
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
            Spacer(Modifier.height(if (compact) 6.dp else 14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Storage,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(if (compact) 14.dp else 16.dp),
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
            Spacer(Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { (mem.swap_used_pct ?: 0f) / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (compact) 3.dp else 4.dp),
                color = MonitorAccent.copy(alpha = 0.6f),
                trackColor = MonitorSurfaceVariant,
            )
        }
        Spacer(Modifier.height(if (compact) 6.dp else 12.dp))
        Sparkline(
            data = spark,
            color = MonitorAccent,
            modifier = Modifier.fillMaxWidth().height(if (compact) 36.dp else 56.dp),
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
                fontSize = if (compact) 26.sp else 34.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
            )
            Spacer(Modifier.width(if (compact) 8.dp else 14.dp))
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
                Text(
                    text = "${b.health ?: "-"} | ${b.technology ?: "-"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                )
            }
        }
        if (sn.thermal.max_c != null) {
            Spacer(Modifier.height(if (compact) 4.dp else 10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Thermostat,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(if (compact) 14.dp else 16.dp),
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
        Spacer(Modifier.height(if (compact) 6.dp else 10.dp))
        Sparkline(
            data = spark,
            color = StatusGood,
            modifier = Modifier.fillMaxWidth().height(if (compact) 36.dp else 56.dp),
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
                    style = if (compact) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                )
            }
            Column(Modifier.weight(1f)) {
                Text("Up", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                Text(
                    text = fmtRate(n.tx_rate_bps),
                    style = if (compact) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                )
            }
        }
        Spacer(Modifier.height(if (compact) 6.dp else 10.dp))
        Sparkline(
            data = sparks[Metric.RX] ?: FloatArray(0),
            color = MonitorAccent,
            modifier = Modifier.fillMaxWidth().height(if (compact) 28.dp else 48.dp),
        )
        Spacer(Modifier.height(4.dp))
        Sparkline(
            data = sparks[Metric.TX] ?: FloatArray(0),
            color = StatusGood,
            modifier = Modifier.fillMaxWidth().height(if (compact) 28.dp else 48.dp),
        )
        Spacer(Modifier.height(if (compact) 4.dp else 8.dp))
        Text(
            text = String.format(
                Locale.US,
                "Total: down %s | up %s",
                fmtKb((n.rx_bytes ?: 0L) / 1024),
                fmtKb((n.tx_bytes ?: 0L) / 1024),
            ),
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary,
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
                    .height(if (compact) 26.dp else 34.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "${index + 1}",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    modifier = Modifier.width(if (compact) 16.dp else 20.dp),
                )
                Text(
                    text = p.name,
                    style = if (compact) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = String.format(Locale.US, "%.0f%%", p.cpu_pct),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    modifier = Modifier.width(if (compact) 44.dp else 56.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.End,
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = String.format(Locale.US, "%.0f%%", p.mem_pct),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    modifier = Modifier.width(if (compact) 40.dp else 48.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.End,
                )
            }
            if (index < sn.procs.size - 1) {
                Spacer(Modifier.height(if (compact) 1.dp else 2.dp))
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
            .padding(horizontal = 8.dp, vertical = 2.dp),
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
