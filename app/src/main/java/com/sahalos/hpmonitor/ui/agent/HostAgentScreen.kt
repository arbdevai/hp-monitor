package com.sahalos.hpmonitor.ui.agent

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sahalos.hpmonitor.agent.HostAgentService
import com.sahalos.hpmonitor.ui.theme.MonitorAccent
import com.sahalos.hpmonitor.ui.theme.MonitorBlack
import com.sahalos.hpmonitor.ui.theme.MonitorSurface
import com.sahalos.hpmonitor.ui.theme.StatusCrit
import com.sahalos.hpmonitor.ui.theme.StatusGood
import com.sahalos.hpmonitor.ui.theme.TextPrimary
import com.sahalos.hpmonitor.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HostAgentScreen(
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val isRunning by HostAgentService.isRunning.collectAsStateWithLifecycle()
    val requestCount by HostAgentService.requestCount.collectAsStateWithLifecycle()
    val localIp by HostAgentService.localIp.collectAsStateWithLifecycle()

    val displayIp = if (localIp.isNotEmpty()) localIp else HostAgentService.getLanIpAddress()
    val serverUrl = "http://$displayIp:8787"

    Scaffold(
        containerColor = MonitorBlack,
        topBar = {
            TopAppBar(
                title = { Text("Mode Host (Target Monitor)") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali ke Dashboard",
                            tint = TextSecondary,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MonitorBlack),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Server Switch Card
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MonitorSurface),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Dns,
                                contentDescription = null,
                                tint = if (isRunning) StatusGood else TextSecondary,
                                modifier = Modifier.size(24.dp),
                            )
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = if (isRunning) "SERVER BERJALAN" else "SERVER NONAKTIF",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = if (isRunning) StatusGood else TextSecondary,
                                )
                                Text(
                                    text = if (isRunning) "Port 8787 • Siap dipantau" else "Ketuk switch untuk menyalakan",
                                    fontSize = 12.sp,
                                    color = TextSecondary,
                                )
                            }
                        }
                        Switch(
                            checked = isRunning,
                            onCheckedChange = { checked ->
                                if (checked) {
                                    HostAgentService.start(context)
                                } else {
                                    HostAgentService.stop(context)
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MonitorAccent,
                                checkedTrackColor = MonitorAccent.copy(alpha = 0.3f),
                            ),
                        )
                    }

                    if (isRunning) {
                        Spacer(Modifier.height(16.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MonitorBlack, RoundedCornerShape(8.dp))
                                .padding(12.dp),
                        ) {
                            Column {
                                Text(
                                    text = "ALAMAT MONITOR UNTUK HP LAIN:",
                                    fontSize = 11.sp,
                                    color = TextSecondary,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Spacer(Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Text(
                                        text = serverUrl,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MonitorAccent,
                                    )
                                    IconButton(
                                        onClick = {
                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            clipboard.setPrimaryClip(ClipData.newPlainText("URL Server", serverUrl))
                                            Toast.makeText(context, "URL disalin", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.size(32.dp),
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ContentCopy,
                                            contentDescription = "Salin URL",
                                            tint = TextSecondary,
                                            modifier = Modifier.size(18.dp),
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "Total request diproses: $requestCount",
                            fontSize = 12.sp,
                            color = TextSecondary,
                        )
                    }
                }
            }

            // Panduan Pemakaian
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MonitorSurface),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "PANDUAN HUBUNGKAN DUA PERANGKAT",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            color = TextSecondary,
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "1. Pastikan HP ini dan HP kedua (J1 Ace) terhubung ke jaringan Wi-Fi lokal yang sama.\n\n" +
                                "2. Nyalakan switch server di atas.\n\n" +
                                "3. Buka aplikasi HP Monitor di HP kedua, masuk ke menu Pengaturan, lalu isi Alamat Server dengan: $serverUrl\n\n" +
                                "4. Tekan Simpan. HP kedua akan langsung menampilkan seluruh telemetri HP ini secara real-time.",
                        fontSize = 13.sp,
                        color = TextPrimary,
                        lineHeight = 20.sp,
                    )
                }
            }
        }
    }
}
