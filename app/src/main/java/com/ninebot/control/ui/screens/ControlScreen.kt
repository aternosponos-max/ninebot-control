package com.ninebot.control.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ninebot.control.ble.ScooterState
import com.ninebot.control.ble.SpeedMode

/**
 * Экран управления: выбор режима скорости (1/2/3 — "передачи") и
 * блокировка/разблокировка движения. Управление крышкой деки сознательно
 * не реализовано — см. README.
 */
@Composable
fun ControlScreen(
    state: ScooterState,
    onSelectMode: (SpeedMode) -> Unit,
    onSetMovementLock: (Boolean) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Режим скорости (передача)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
        items(SpeedMode.values().size) { index ->
            val mode = SpeedMode.values()[index]
            val selected = state.speedMode == mode
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(text = mode.label, fontWeight = FontWeight.Bold)
                        Text(text = mode.approxKmh, style = MaterialTheme.typography.bodySmall)
                    }
                    if (selected) {
                        Text(text = "Активен", color = MaterialTheme.colorScheme.primary)
                    } else {
                        OutlinedButton(onClick = { onSelectMode(mode) }) {
                            Text("Выбрать")
                        }
                    }
                }
            }
        }

        item {
            Text(
                text = "Блокировка движения",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = if (state.movementLocked) "Заблокировано" else "Разблокировано",
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Запрещает движение (не влияет на крышку деки)",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Switch(
                        checked = state.movementLocked,
                        onCheckedChange = { onSetMovementLock(it) }
                    )
                }
            }
        }
    }
}
