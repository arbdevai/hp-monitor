package com.sahalos.hpmonitor.ui.dashboard

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
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Thermostat
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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

private fun fmtRate(bps: Long?): String {
    val v = bps ?: return "--"
    return when {
        v < 1024 -> "${v} B/s"
        v < 1024 * 1024 -> String.format(Locale.US, "%.1f KB/s", v / 1024.0)
        else -> String.format(Locale.US, "%.2f MB/s", v / (1024.0 * 1024.0))
    }
}

private fun fmtKb(kb: Long): String {
    return when {
        kb < 1024 -> "${kb} KB"
        kb < 1024 * 1024 -> String.format(Locale.US, "%.1f MB", kb / 1024.0)
        else -> String.format(Locale.US, "%.2f GB", kb / (1024.0 * 1024.0))
    }
}

private fun fmtUptime(s: Long): String {
    val d = s / 86400
    val h = (s % 86400) / 3600
    val m = (s % 3600) / 60
    return when {
        d > 0 -> "${d}h ${h}j ${m}m"
        h > 0 -> "${h}j ${m}m"
        else -> "${m}m ${s % 60}s"
    }
}

private fun fmtClock(ts: Long): String =
    SimpleDateFormat("HH:mm:ss", Locale.US).format(Date(ts))

@Composable
fun StatusChip(text: String, color: Color) {
    Box(
        modifier = Modifier
            .background(color.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text(
            text = text,
            color = color,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    vm: DashboardViewModel,
    onOpenSettings: () -> Unit,
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val snapshot = state.snapshot

    Scaffold(
        containerColor = MonitorBlack,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("HP Monitor", fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.width(10.dp))
                        StatusChip(
                            text = if (state.connected) "ONLINE" else "OFFLINE",
                            color = if (state.connected) StatusGood else StatusCrit,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MonitorBlack),
                actions = {
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
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
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
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (!state.connected) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MonitorSurfaceVariant, RoundedCornerShape(10.dp))
                        .padding(12.dp),
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
                HeaderRow(sn, state.lastUpdated)
                CpuCard(sn, state.sparks[Metric.CPU] ?: FloatArray(0))
                MemoryCard(sn, state.sparks[Metric.MEM] ?: FloatArray(0))
                BatteryCard(sn, state.sparks[Metric.BATTERY] ?: FloatArray(0))
                NetworkCard(sn, state.sparks)
                ProcessesCard(sn)
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
private fun HeaderRow(sn: MetricsSnapshot, lastUpdated: Long) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = sn.hostname.ifEmpty { "Device target" },
                style = MaterialTheme.typography.titleMedium,
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
private fun CpuCard(sn: MetricsSnapshot, spark: FloatArray) {
    MetricCard(title = "CPU", icon = Icons.Default.DeveloperBoard) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = sn.cpu.overall_pct?.let { String.format(Locale.US, "%.0f", it) } ?: "--",
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
            )
            Text(" %", fontSize = 18.sp, color = TextSecondary)
            Spacer(Modifier.weight(1f))
            val load = sn.cpu.load
            if (load.size >= 3) {
                Text(
                    text = String.format(
                        Locale.US,
                        "Load %.1f / %.1f / %.1f",
                        load[0],
                        load[1],
                        load[2],
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        // Per-core mini bars
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(26.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            val cores = sn.cpu.per_core_pct
            for ((i, pct) in cores.withIndex()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .background(MonitorSurfaceVariant, RoundedCornerShape(3.dp)),
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
        Spacer(Modifier.height(10.dp))
        Sparkline(data = spark, color = MonitorAccent)
    }
}

@Composable
private fun MemoryCard(sn: MetricsSnapshot, spark: FloatArray) {
    val mem = sn.mem
    MetricCard(title = "Memori", icon = Icons.Default.Memory) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "${fmtKb(mem.used_kb)} / ${fmtKb(mem.total_kb)}",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = mem.used_pct?.let { String.format(Locale.US, "%.0f%%", it) } ?: "--",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
            )
        }
        Spacer(Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { (mem.used_pct ?: 0f) / 100f },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp),
            color = MonitorAccent,
            trackColor = MonitorSurfaceVariant,
        )
        Spacer(Modifier.height(14.dp))
        if (mem.swap_total_kb > 0) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Storage,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(6.dp))
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
            Spacer(Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { (mem.swap_used_pct ?: 0f) / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp),
                color = MonitorAccent.copy(alpha = 0.6f),
                trackColor = MonitorSurfaceVariant,
            )
        }
        Spacer(Modifier.height(12.dp))
        Sparkline(data = spark, color = MonitorAccent)
    }
}

@Composable
private fun BatteryCard(sn: MetricsSnapshot, spark: FloatArray) {
    val b = sn.battery
    MetricCard(title = "Baterai & Termal", icon = Icons.Default.BatteryFull) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = b.capacity_pct?.toString()?.plus("%") ?: "--",
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
            )
            Spacer(Modifier.width(14.dp))
            Column {
                Text(
                    text = b.status ?: "Tidak diketahui",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary,
                )
                val v = b.voltage_mv?.toLongOrNull()
                Text(
                    text = (b.temperature_c?.let { String.format(Locale.US, "%.1f C", it) })
                        .plus(if (v != null) "  |  ${String.format(Locale.US, "%.2f V", v / 1000.0)}" else ""),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                )
                Text(
                    text = "Kesehatan: ${b.health ?: "-"}  |  ${b.technology ?: "-"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        if (sn.thermal.max_c != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Thermostat,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(6.dp))
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
        Spacer(Modifier.height(10.dp))
        Sparkline(data = spark, color = StatusGood)
    }
}

@Composable
private fun NetworkCard(sn: MetricsSnapshot, sparks: Map<Int, FloatArray>) {
    val n = sn.net
    MetricCard(title = "Jaringan (wlan0)", icon = Icons.Default.Speed) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Down", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                Text(
                    text = fmtRate(n.rx_rate_bps),
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                )
            }
            Column(Modifier.weight(1f)) {
                Text("Up", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                Text(
                    text = fmtRate(n.tx_rate_bps),
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        Sparkline(data = sparks[Metric.RX] ?: FloatArray(0), color = MonitorAccent)
        Spacer(Modifier.height(6.dp))
        Sparkline(data = sparks[Metric.TX] ?: FloatArray(0), color = StatusGood)
        Spacer(Modifier.height(8.dp))
        Text(
            text = String.format(
                Locale.US,
                "Total sesi: down %s | up %s",
                fmtKb((n.rx_bytes ?: 0L) / 1024),
                fmtKb((n.tx_bytes ?: 0L) / 1024),
            ),
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary,
        )
    }
}

@Composable
private fun ProcessesCard(sn: MetricsSnapshot) {
    MetricCard(title = "Proses Teratas (CPU)", icon = Icons.Default.DeveloperBoard) {
        sn.procs.forEachIndexed { index, p ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(34.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "${index + 1}",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    modifier = Modifier.width(20.dp),
                )
                Text(
                    text = p.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = String.format(Locale.US, "%.0f%%", p.cpu_pct),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    modifier = Modifier.width(56.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.End,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = String.format(Locale.US, "%.0f%%", p.mem_pct),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    modifier = Modifier.width(48.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.End,
                )
            }
            if (index < sn.procs.size - 1) {
                Spacer(Modifier.height(2.dp))
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
