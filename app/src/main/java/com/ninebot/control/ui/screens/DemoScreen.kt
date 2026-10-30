package com.ninebot.control.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Science
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ninebot.control.ble.ScooterState
import com.ninebot.control.ble.SpeedMode
import com.ninebot.control.ble.TuningProfile

/**
 * Demo-Mode: полный набор экранов на симулированных данных.
 * Переключатели здесь меняют только демонстрационное состояние и не посылают BLE-команды.
 */
@Composable
fun DemoScreen(
    state: ScooterState,
    onSelectMode: (SpeedMode) -> Unit,
    onSetMovementLock: (Boolean) -> Unit,
    onSelectProfile: (TuningProfile) -> Unit,
    onSetPhaseCurrent: (Int) -> Unit,
    onSetBatteryCurrent: (Int) -> Unit,
    onSetSpeedLimitOverride: (Int) -> Unit,
    onSetFieldWeakening: (Boolean) -> Unit,
    onSetCruiseControl: (Boolean) -> Unit,
    onSetRegenCurrent: (Int) -> Unit,
    onSetBrakeBoost: (Int) -> Unit,
    onSetThrottleBoost: (Int) -> Unit,
    onSetAccelBoost: (Int) -> Unit,
    onRefreshDiagnostics: () -> Unit
) {
    var tab by remember { mutableIntStateOf(0) }
    var demoDeckOpen by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f))
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Filled.Science, contentDescription = null)
                Text(
                    text = "  ДЕМО-РЕЖИМ — данные симулируются, самокат не подключён",
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }

        ScrollableTabRow(selectedTabIndex = tab) {
            Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Приборная панель") })
            Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Управление") })
            Tab(selected = tab == 2, onClick = { tab = 2 }, text = { Text("Тюнинг") })
            Tab(selected = tab == 3, onClick = { tab = 3 }, text = { Text("Статистика") })
            Tab(selected = tab == 4, onClick = { tab = 4 }, text = { Text("Замок деки") })
        }

        when (tab) {
            0 -> DashboardScreen(state = state)
            1 -> ControlScreen(
                state = state,
                onSelectMode = onSelectMode,
                onSetMovementLock = onSetMovementLock
            )
            2 -> TuningScreen(
                state = state,
                onSelectProfile = onSelectProfile,
                onSetPhaseCurrent = onSetPhaseCurrent,
                onSetBatteryCurrent = onSetBatteryCurrent,
                onSetSpeedLimitOverride = onSetSpeedLimitOverride,
                onSetFieldWeakening = onSetFieldWeakening,
                onSetCruiseControl = onSetCruiseControl,
                onSetRegenCurrent = onSetRegenCurrent,
                onSetBrakeBoost = onSetBrakeBoost,
                onSetThrottleBoost = onSetThrottleBoost,
                onSetAccelBoost = onSetAccelBoost
            )
            3 -> StatisticsScreen(state = state, onRefresh = onRefreshDiagnostics)
            4 -> Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = if (demoDeckOpen) Icons.Filled.LockOpen else Icons.Filled.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = if (demoDeckOpen) "Замок деки открыт" else "Замок деки закрыт",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = "Демонстрация интерфейса. Команда на контроллер не отправляется.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(if (demoDeckOpen) "Открыт" else "Закрыт")
                            Switch(
                                checked = demoDeckOpen,
                                onCheckedChange = { demoDeckOpen = it }
                            )
                        }
                        Button(onClick = { demoDeckOpen = !demoDeckOpen }) {
                            Text(if (demoDeckOpen) "Закрыть замок (демо)" else "Открыть замок (демо)")
                        }
                    }
                }
                Text(
                    text = "Реальное управление замком будет доступно после подтверждения команды для конкретной версии контроллера и прошивки.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
