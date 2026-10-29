package com.ninebot.control.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ninebot.control.ble.ConnectionStatus
import com.ninebot.control.ble.ScooterState

data class SimpleDevice(val name: String, val address: String)

@Composable
fun ConnectScreen(
    state: ScooterState,
    devices: List<SimpleDevice>,
    onStartScan: () -> Unit,
    onStopScan: () -> Unit,
    onConnect: (SimpleDevice) -> Unit,
    onDisconnect: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            text = "Статус: ${statusLabel(state.connection)}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        if (state.deviceName != null) {
            Text(text = "Устройство: ${state.deviceName}", style = MaterialTheme.typography.bodyMedium)
        }

        androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(8.dp))

        if (state.connection == ConnectionStatus.CONNECTED) {
            Button(onClick = onDisconnect) { Text("Отключиться") }
        } else if (state.connection == ConnectionStatus.SCANNING) {
            Button(onClick = onStopScan) { Text("Остановить поиск") }
        } else {
            Button(onClick = onStartScan) { Text("Найти самокат (BLE)") }
        }

        androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(8.dp))

        Text(text = "Найденные устройства", style = MaterialTheme.typography.titleSmall)
        LazyColumn(
            contentPadding = PaddingValues(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(devices) { device ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(text = device.name, fontWeight = FontWeight.Bold)
                        Text(text = device.address, style = MaterialTheme.typography.bodySmall)
                        OutlinedButton(onClick = { onConnect(device) }) {
                            Text("Подключиться")
                        }
                    }
                }
            }
        }

        if (state.log.isNotEmpty()) {
            Text(text = "Журнал (последние пакеты)", style = MaterialTheme.typography.titleSmall)
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                items(state.log.takeLast(50).reversed()) { line ->
                    Text(text = line, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

private fun statusLabel(status: ConnectionStatus): String = when (status) {
    ConnectionStatus.DISCONNECTED -> "Отключено"
    ConnectionStatus.SCANNING -> "Поиск устройств…"
    ConnectionStatus.CONNECTING -> "Подключение…"
    ConnectionStatus.CONNECTED -> "Подключено"
    ConnectionStatus.ERROR -> "Ошибка"
}
