package com.ninebot.control

import android.bluetooth.BluetoothDevice
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.ninebot.control.ble.NinebotBleManager
import com.ninebot.control.demo.DemoSimulator
import com.ninebot.control.ui.screens.ConnectScreen
import com.ninebot.control.ui.screens.ControlScreen
import com.ninebot.control.ui.screens.DashboardScreen
import com.ninebot.control.ui.screens.DemoScreen
import com.ninebot.control.ui.screens.SimpleDevice
import com.ninebot.control.ui.screens.StatisticsScreen
import com.ninebot.control.ui.screens.TuningScreen

private sealed class Dest(val route: String, val label: String, val icon: ImageVector) {
    data object Connect : Dest("connect", "Подключение", Icons.Filled.Bluetooth)
    data object Control : Dest("control", "Управление", Icons.Filled.Tune)
    data object Dashboard : Dest("dashboard", "Панель", Icons.Filled.Dashboard)
    data object Tuning : Dest("tuning", "Тюнинг", Icons.Filled.Speed)
    data object Statistics : Dest("statistics", "Статистика", Icons.Filled.QueryStats)
    data object Demo : Dest("demo", "Demo-Mode", Icons.Filled.Science)
}

private val destinations =
    listOf(Dest.Connect, Dest.Control, Dest.Dashboard, Dest.Tuning, Dest.Statistics, Dest.Demo)

@Composable
fun NinebotApp(bleManager: NinebotBleManager, demoSimulator: DemoSimulator) {
    val navController = rememberNavController()
    val realState by bleManager.state.collectAsState()
    val demoState by demoSimulator.state.collectAsState()
    val discovered by bleManager.discoveredDevices.collectAsState()

    Scaffold(
        bottomBar = {
            val backStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = backStackEntry?.destination?.route
            NavigationBar {
                destinations.forEach { dest ->
                    NavigationBarItem(
                        selected = currentRoute == dest.route,
                        onClick = {
                            navController.navigate(dest.route) {
                                launchSingleTop = true
                            }
                        },
                        icon = { Icon(dest.icon, contentDescription = dest.label) },
                        label = { Text(dest.label) }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Dest.Connect.route,
            modifier = Modifier.padding(padding)
        ) {
            composable(Dest.Connect.route) {
                ConnectScreen(
                    state = realState,
                    devices = discovered.map { d: BluetoothDevice ->
                        SimpleDevice(name = d.name ?: "Без имени", address = d.address)
                    },
                    onStartScan = { bleManager.startScan() },
                    onStopScan = { bleManager.stopScan() },
                    onConnect = { simple ->
                        val device = discovered.firstOrNull { it.address == simple.address }
                        if (device != null) bleManager.connect(device)
                    },
                    onDisconnect = { bleManager.disconnect() }
                )
            }
            composable(Dest.Control.route) {
                ControlScreen(
                    state = realState,
                    onSelectMode = { bleManager.sendSetSpeedMode(it) },
                    onSetMovementLock = { bleManager.sendSetMovementLock(it) }
                )
            }
            composable(Dest.Dashboard.route) {
                DashboardScreen(state = realState)
            }
            composable(Dest.Tuning.route) {
                TuningScreen(
                    state = realState,
                    onSetCurrentLimit = { bleManager.sendSetCurrentLimit(it) },
                    onSetSpeedLimitOverride = { bleManager.sendSetSpeedLimitOverride(it) },
                    onSetFieldWeakening = { bleManager.sendSetFieldWeakening(it) },
                    onSetCruiseControl = { bleManager.sendSetCruiseControl(it) },
                    onSetRegenBraking = { bleManager.sendSetRegenBraking(it) }
                )
            }
            composable(Dest.Statistics.route) {
                StatisticsScreen(
                    state = realState,
                    onRefresh = { bleManager.requestDiagnostics() }
                )
            }
            composable(Dest.Demo.route) {
                DemoScreen(
                    state = demoState,
                    onSelectMode = { demoSimulator.setSpeedMode(it) },
                    onSetMovementLock = { demoSimulator.setMovementLock(it) },
                    onSetCurrentLimit = { demoSimulator.setCurrentLimit(it) },
                    onSetSpeedLimitOverride = { demoSimulator.setSpeedLimitOverride(it) },
                    onSetFieldWeakening = { demoSimulator.setFieldWeakening(it) },
                    onSetCruiseControl = { demoSimulator.setCruiseControl(it) },
                    onSetRegenBraking = { demoSimulator.setRegenBraking(it) },
                    onRefreshDiagnostics = { }
                )
            }
        }
    }
}
