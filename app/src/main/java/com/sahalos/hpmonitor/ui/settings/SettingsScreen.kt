package com.sahalos.hpmonitor.ui.settings

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sahalos.hpmonitor.data.Preferences
import com.sahalos.hpmonitor.ui.dashboard.DashboardViewModel
import com.sahalos.hpmonitor.ui.theme.MonitorAccent
import com.sahalos.hpmonitor.ui.theme.MonitorBlack
import com.sahalos.hpmonitor.ui.theme.TextPrimary
import com.sahalos.hpmonitor.ui.theme.TextSecondary
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    vm: DashboardViewModel,
    onBack: () -> Unit,
) {
    val state by vm.state.collectAsStateWithLifecycle()
    var url by remember(state.serverUrl) { mutableStateOf(state.serverUrl) }

    Scaffold(
        containerColor = MonitorBlack,
        topBar = {
            TopAppBar(
                title = { Text("Pengaturan") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali",
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
        ) {
            Text(
                "SERVER MONITOR",
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary,
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = url,
                onValueChange = { url = it },
                label = { Text("Alamat server (IP:port)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Contoh: ${Preferences.DEFAULT_URL}",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
            )
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = { vm.applyUrl(url) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Simpan")
            }

            Spacer(Modifier.height(24.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            Spacer(Modifier.height(24.dp))

            Text(
                "TAMPILAN",
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary,
            )
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "Tampilan Ringkas (Compact)",
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextPrimary,
                    )
                    Text(
                        "Optimasi kepadatan informasi untuk layar kecil & lanskap",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                    )
                }
                Switch(
                    checked = state.compactMode,
                    onCheckedChange = { vm.toggleCompactMode() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MonitorAccent,
                        checkedTrackColor = MonitorAccent.copy(alpha = 0.3f),
                    ),
                )
            }

            Spacer(Modifier.height(24.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            Spacer(Modifier.height(24.dp))

            Text(
                "APLIKASI",
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary,
            )
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth()) {
                Text(
                    "Versi terpasang",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    state.currentVersion,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                )
            }
            Spacer(Modifier.height(12.dp))

            UpdateSection(vm = vm)
        }
    }
}

@Composable
private fun UpdateSection(vm: DashboardViewModel) {
    val state by vm.state.collectAsStateWithLifecycle()
    val ui = state.update
    val busy = ui.state == "checking" || ui.state == "downloading"

    when (ui.state) {
        "checking" -> Button(
            onClick = {},
            enabled = false,
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Memeriksa pembaruan...") }

        "available" -> Button(
            onClick = { vm.downloadUpdate() },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Unduh v${state.updateInfo?.tag}")
        }

        "downloading" -> Column {
            LinearProgressIndicator(
                progress = { ui.progress },
                modifier = Modifier.fillMaxWidth(),
                color = MonitorAccent,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                String.format(Locale.US, "Mengunduh... %.0f%%", ui.progress * 100f),
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
            )
        }

        "ready" -> Button(
            onClick = { vm.installPendingUpdate() },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Pasang Pembaruan") }

        else -> Button(
            onClick = { vm.checkForUpdate() },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Periksa Pembaruan")
        }
    }

    if (ui.message.isNotEmpty() && ui.state != "downloading") {
        Spacer(Modifier.height(8.dp))
        Text(
            ui.message,
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
        )
    }
}
