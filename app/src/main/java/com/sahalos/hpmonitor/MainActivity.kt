package com.sahalos.hpmonitor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.sahalos.hpmonitor.ui.agent.HostAgentScreen
import com.sahalos.hpmonitor.ui.dashboard.DashboardScreen
import com.sahalos.hpmonitor.ui.dashboard.DashboardViewModel
import com.sahalos.hpmonitor.ui.settings.SettingsScreen
import com.sahalos.hpmonitor.ui.theme.HpMonitorTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val vm: DashboardViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Polling loop is lifecycle-bound: it cancels automatically when the
        // activity drops below STARTED (screen off / background), so the
        // monitor never polls idle and burns battery.
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                vm.runLoop()
            }
        }

        setContent {
            val state by vm.state.collectAsStateWithLifecycle()
            val view = LocalView.current

            DisposableEffect(state.keepScreenOn) {
                view.keepScreenOn = state.keepScreenOn
                onDispose {
                    view.keepScreenOn = false
                }
            }

            HpMonitorTheme {
                MonitorApp(vm)
            }
        }
    }
}

private enum class Screen { DASHBOARD, HOST_AGENT, SETTINGS }

@Composable
private fun MonitorApp(vm: DashboardViewModel) {
    var screen by rememberSaveable { mutableStateOf(Screen.DASHBOARD) }

    AnimatedContent(
        targetState = screen,
        transitionSpec = {
            (androidx.compose.animation.slideInHorizontally { it / 4 } + fadeIn()).togetherWith(
                (androidx.compose.animation.slideOutHorizontally { -it / 4 } + fadeOut())
            )
        },
        label = "screenTransition",
    ) { target ->
        when (target) {
            Screen.DASHBOARD -> DashboardScreen(
                vm = vm,
                onOpenSettings = { screen = Screen.SETTINGS },
                onOpenHost = { screen = Screen.HOST_AGENT },
            )
            Screen.HOST_AGENT -> HostAgentScreen(
                onBack = { screen = Screen.DASHBOARD },
            )
            Screen.SETTINGS -> SettingsScreen(
                vm = vm,
                onBack = { screen = Screen.DASHBOARD },
            )
        }
    }
}
