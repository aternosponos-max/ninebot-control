package com.ninebot.control.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ninebot.control.ble.ScooterState
import com.ninebot.control.ui.components.MetricCard
import com.ninebot.control.ui.components.SpeedGauge
import com.ninebot.control.ui.theme.BrandBlue
import com.ninebot.control.ui.theme.BrandGreen
import com.ninebot.control.ui.theme.BrandOrange
import kotlin.math.roundToInt

/**
 * Приборная панель с категориями: Скорость, Мощность, Батарея, Пробег.
 * Используется и для реального подключения, и для демо-режима —
 * им подаётся один и тот же [ScooterState].
 */
@Composable
fun DashboardScreen(state: ScooterState) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Скорость",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
        item {
            SpeedGauge(speedKmh = state.speedKmh)
        }
        item {
            Text(
                text = "Мощность",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
        item {
            val powerZoneColor = when {
                state.powerWatts < 150 -> BrandGreen
                state.powerWatts < 400 -> BrandBlue
                else -> BrandOrange
            }
            val zoneLabel = when {
                state.powerWatts < 150 -> "Эко"
                state.powerWatts < 400 -> "Норма"
                else -> "Пик"
            }
            MetricCard(
                title = "Текущая мощность",
                value = "${state.powerWatts} Вт",
                subtitle = "Зона: $zoneLabel",
                accentColor = powerZoneColor
            )
        }
        item {
            Text(
                text = "Батарея",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricCard(
                    title = "Заряд",
                    value = "${state.batteryPercent}%",
                    accentColor = if (state.batteryPercent < 20) BrandOrange else BrandGreen,
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = "Напряжение",
                    value = String.format("%.1f В", state.batteryVoltage),
                    modifier = Modifier.weight(1f)
                )
            }
        }
        item {
            MetricCard(
                title = "Температура батареи",
                value = "${state.batteryTemperatureC}°C",
                accentColor = if (state.batteryTemperatureC > 45) BrandOrange else BrandBlue
            )
        }
        item {
            Text(
                text = "Пробег",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricCard(
                    title = "За поездку",
                    value = String.format("%.2f км", state.tripDistanceKm),
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = "Всего",
                    value = String.format("%.1f км", state.totalDistanceKm),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
