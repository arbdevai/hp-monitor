package com.sahalos.hpmonitor.data

import kotlinx.serialization.Serializable

@Serializable
data class CpuInfo(
    val overall_pct: Float? = null,
    val per_core_pct: List<Float> = emptyList(),
    val cores: Int = 0,
    val load: List<Float> = emptyList(),
)

@Serializable
data class MemInfo(
    val total_kb: Long = 0,
    val used_kb: Long = 0,
    val available_kb: Long = 0,
    val used_pct: Float? = null,
    val swap_total_kb: Long = 0,
    val swap_used_kb: Long = 0,
    val swap_used_pct: Float? = null,
)

@Serializable
data class BatteryInfo(
    val present: Boolean = false,
    val capacity_pct: Int? = null,
    val temperature_c: Float? = null,
    val status: String? = null,
    val health: String? = null,
    val voltage_mv: String? = null,
    val technology: String? = null,
)

@Serializable
data class ThermalInfo(
    val max_c: Float? = null,
    val zone: String? = null,
)

@Serializable
data class NetInfo(
    val iface: String? = null,
    val rx_bytes: Long? = null,
    val tx_bytes: Long? = null,
    val rx_rate_bps: Long? = null,
    val tx_rate_bps: Long? = null,
)

@Serializable
data class ProcInfo(
    val pid: Int = 0,
    val name: String = "",
    val cpu_pct: Float = 0f,
    val mem_pct: Float = 0f,
    val rss_mb: Float = 0f,
)

@Serializable
data class MetricsSnapshot(
    val ts: Long = 0,
    val hostname: String = "",
    val uptime_s: Long = 0,
    val cpu: CpuInfo = CpuInfo(),
    val mem: MemInfo = MemInfo(),
    val battery: BatteryInfo = BatteryInfo(),
    val thermal: ThermalInfo = ThermalInfo(),
    val net: NetInfo = NetInfo(),
    val procs: List<ProcInfo> = emptyList(),
)
