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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ninebot.control.ble.ScooterState
import com.ninebot.control.ui.components.MetricCard
import com.ninebot.control.ui.theme.BrandGreen
import com.ninebot.control.ui.theme.BrandOrange
import com.ninebot.control.ui.theme.BrandRed

/**
 * Статистика и диагностика в духе сторонних дашборд-приложений
 * (Ninedash и подобные): напряжение по ячейкам батареи, коды ошибок
 * контроллера, версия прошивки/серийный номер, статистика текущей поездки.
 */
@Composable
fun StatisticsScreen(
    state: ScooterState,
    onRefresh: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Button(onClick = onRefresh, modifier = Modifier.fillMaxWidth()) {
                Text("Обновить диагностику")
            }
        }

        item {
            Text(
                text = "Поездка",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricCard(
                    title = "Время в движении",
                    value = formatDuration(state.rideTimeSeconds),
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = "Средняя скорость",
                    value = String.format("%.1f км/ч", state.avgSpeedSessionKmh),
                    modifier = Modifier.weight(1f)
                )
            }
        }
        item {
            MetricCard(
                title = "Максимальная скорость за поездку",
                value = String.format("%.1f км/ч", state.maxSpeedSessionKmh)
            )
        }

        item {
            Text(
                text = "Батарея — напряжение по ячейкам",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
        if (state.cellVoltages.isEmpty()) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Нет данных. Нажмите «Обновить диагностику» после подключения.",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        } else {
            val minV = state.cellVoltages.min()
            val maxV = state.cellVoltages.max()
            val spreadMv = ((maxV - minV) * 1000).toInt()
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    MetricCard(
                        title = "Мин / Макс ячейка",
                        value = String.format("%.3f / %.3f В", minV, maxV),
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = "Разброс",
                        value = "$spreadMv мВ",
                        accentColor = if (spreadMv > 50) BrandOrange else BrandGreen,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        state.cellVoltages.forEachIndexed { index, v ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = "Ячейка ${index + 1}", style = MaterialTheme.typography.bodySmall)
                                Text(text = String.format("%.3f В", v), style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }

        item {
            Text(
                text = "Ошибки контроллера",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    if (state.errorCodes.isEmpty()) {
                        Text(text = "Ошибок нет", color = BrandGreen)
                    } else {
                        state.errorCodes.forEach { code ->
                            Text(text = code, color = BrandRed)
                        }
                    }
                }
            }
        }

        item {
            Text(
                text = "Прошивка и устройство",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricCard(
                    title = "Версия прошивки",
                    value = state.firmwareVersion,
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = "Серийный номер",
                    value = state.serialNumber,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

private fun formatDuration(totalSeconds: Long): String {
    val h = totalSeconds / 3600
    val m = (totalSeconds % 3600) / 60
    val s = totalSeconds % 60
    return if (h > 0) String.format("%d:%02d:%02d", h, m, s) else String.format("%d:%02d", m, s)
}
