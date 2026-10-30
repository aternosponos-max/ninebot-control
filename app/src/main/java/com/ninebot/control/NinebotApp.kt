package com.ninebot.control

import android.bluetooth.BluetoothDevice
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.ninebot.control.ble.NinebotBleManager
import com.ninebot.control.demo.DemoSimulator
import com.ninebot.control.ui.screens.AuthorsScreen
import com.ninebot.control.ui.screens.ConnectScreen
import com.ninebot.control.ui.screens.ControlScreen
import com.ninebot.control.ui.screens.DashboardScreen
import com.ninebot.control.ui.screens.DemoScreen
import com.ninebot.control.ui.screens.SimpleDevice
import com.ninebot.control.ui.screens.StatisticsScreen
import com.ninebot.control.ui.screens.TuningScreen
import com.ninebot.control.ui.theme.ThemePalette

private sealed class Dest(val route: String, val label: String, val icon: ImageVector) {
    data object Connect : Dest("connect", "Подключение", Icons.Filled.Bluetooth)
    data object Control : Dest("control", "Управление", Icons.Filled.Tune)
    data object Dashboard : Dest("dashboard", "Панель", Icons.Filled.Dashboard)
    data object Tuning : Dest("tuning", "Тюнинг", Icons.Filled.Speed)
    data object Statistics : Dest("statistics", "Статистика", Icons.Filled.QueryStats)
}

// Demo-Mode и Авторы убраны из нижней навигации — доступны через меню "⋮"
// в правом верхнем углу, чтобы оставить снизу больше места под основные вкладки.
private const val DEMO_ROUTE = "demo"
private const val AUTHORS_ROUTE = "authors"

private val bottomDestinations =
    listOf(Dest.Connect, Dest.Control, Dest.Dashboard, Dest.Tuning, Dest.Statistics)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NinebotApp(bleManager: NinebotBleManager, demoSimulator: DemoSimulator, themePalette: ThemePalette, onThemePaletteChange: (ThemePalette) -> Unit) {
    val navController = rememberNavController()
    val realState by bleManager.state.collectAsState()
    val demoState by demoSimulator.state.collectAsState()
    val discovered by bleManager.discoveredDevices.collectAsState()
    var menuExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ninebot Control") },
                actions = {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "Ещё")
                    }
                    DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                        DropdownMenuItem(
                            text = { Text("Demo-Mode") },
                            onClick = {
                                menuExpanded = false
                                navController.navigate(DEMO_ROUTE) { launchSingleTop = true }
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Авторы") },
                            onClick = {
                                menuExpanded = false
                                navController.navigate(AUTHORS_ROUTE) { launchSingleTop = true }
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Цветовая тема") },
                            onClick = { menuExpanded = true }
                        )
                        ThemePalette.values().forEach { palette ->
                            DropdownMenuItem(
                                text = {
                                    Text(if (themePalette == palette) "✓ ${palette.label}" else palette.label)
                                },
                                onClick = {
                                    onThemePaletteChange(palette)
                                    menuExpanded = false
                                }
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            val backStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = backStackEntry?.destination?.route
            NavigationBar {
                bottomDestinations.forEach { dest ->
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
                    onSelectProfile = { bleManager.sendSelectProfile(it) },
                    onSetPhaseCurrent = { bleManager.sendSetPhaseCurrent(it) },
                    onSetBatteryCurrent = { bleManager.sendSetBatteryCurrent(it) },
                    onSetSpeedLimitOverride = { bleManager.sendSetSpeedLimitOverride(it) },
                    onSetFieldWeakening = { bleManager.sendSetFieldWeakening(it) },
                    onSetCruiseControl = { bleManager.sendSetCruiseControl(it) },
                    onSetRegenCurrent = { bleManager.sendSetRegenCurrent(it) },
                    onSetBrakeBoost = { bleManager.sendSetBrakeBoost(it) },
                    onSetThrottleBoost = { bleManager.sendSetThrottleBoost(it) },
                    onSetAccelBoost = { bleManager.sendSetAccelBoost(it) }
                )
            }
            composable(Dest.Statistics.route) {
                StatisticsScreen(
                    state = realState,
                    onRefresh = { bleManager.requestDiagnostics() }
                )
            }
            composable(DEMO_ROUTE) {
                DemoScreen(
                    state = demoState,
                    onSelectMode = { demoSimulator.setSpeedMode(it) },
                    onSetMovementLock = { demoSimulator.setMovementLock(it) },
                    onSelectProfile = { demoSimulator.selectProfile(it) },
                    onSetPhaseCurrent = { demoSimulator.setPhaseCurrent(it) },
                    onSetBatteryCurrent = { demoSimulator.setBatteryCurrent(it) },
                    onSetSpeedLimitOverride = { demoSimulator.setSpeedLimitOverride(it) },
                    onSetFieldWeakening = { demoSimulator.setFieldWeakening(it) },
                    onSetCruiseControl = { demoSimulator.setCruiseControl(it) },
                    onSetRegenCurrent = { demoSimulator.setRegenCurrent(it) },
                    onSetBrakeBoost = { demoSimulator.setBrakeBoost(it) },
                    onSetThrottleBoost = { demoSimulator.setThrottleBoost(it) },
                    onSetAccelBoost = { demoSimulator.setAccelBoost(it) },
                    onRefreshDiagnostics = { }
                )
            }
            composable(AUTHORS_ROUTE) {
                AuthorsScreen()
            }
        }
    }
}
