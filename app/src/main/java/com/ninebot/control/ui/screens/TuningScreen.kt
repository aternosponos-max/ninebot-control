package com.ninebot.control.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ninebot.control.ble.ScooterState
import com.ninebot.control.ble.TuningLimits
import com.ninebot.control.ble.TuningProfile
import com.ninebot.control.ui.theme.BrandOrange
import com.ninebot.control.ui.theme.BrandRed

/**
 * Расширенные параметры тюнинга (аналог функций DRV и подобных кастомных
 * прошивок): фазный и батарейный ток, override максимальной скорости,
 * field weakening, круиз-контроль, ток рекуперации, бусты и профили.
 *
 * Работает только с самокатами, где уже установлена совместимая кастомная
 * прошивка контроллера — заводская/шеринговая прошивка эти регистры не
 * поддерживает, запись будет проигнорирована контроллером.
 */
@Composable
fun TuningScreen(
    state: ScooterState,
    onSelectProfile: (TuningProfile) -> Unit,
    onSetPhaseCurrent: (Int) -> Unit,
    onSetBatteryCurrent: (Int) -> Unit,
    onSetSpeedLimitOverride: (Int) -> Unit,
    onSetFieldWeakening: (Boolean) -> Unit,
    onSetCruiseControl: (Boolean) -> Unit,
    onSetRegenCurrent: (Int) -> Unit,
    onSetBrakeBoost: (Int) -> Unit,
    onSetThrottleBoost: (Int) -> Unit,
    onSetAccelBoost: (Int) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.padding(12.dp)) {
                    Icon(
                        imageVector = Icons.Filled.WarningAmber,
                        contentDescription = null,
                        tint = BrandRed
                    )
                    Text(
                        text = "  Эти параметры действуют только на контроллерах с установленной " +
                            "совместимой кастомной прошивкой (DRV и аналоги). Верхние пределы " +
                            "тока на этом экране подняты до 120 А по явному запросу владельца " +
                            "под усиленный контроллер. Это значительно выше типичных значений " +
                            "даже для доработанного железа этой линейки (сообщество обычно " +
                            "держится в районе 40–60 А) — ответственность за соответствие " +
                            "реальным возможностям вашего конкретного контроллера лежит на вас. " +
                            "Поднимайте значения постепенно и следите за температурой мотора, " +
                            "контроллера и батареи.",
                        style = MaterialTheme.typography.bodySmall,
                        color = BrandRed
                    )
                }
            }
        }

        item {
            Text(
                text = "Профиль прошивки",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TuningProfile.values().forEach { profile ->
                    FilterChip(
                        selected = state.tuningProfile == profile,
                        onClick = { onSelectProfile(profile) },
                        label = { Text(profile.label) }
                    )
                }
            }
        }
        item {
            Text(
                text = "Выбор готового профиля сразу применяет его значения ко всем параметрам " +
                    "ниже. Ручное изменение любого параметра переключает профиль на «Свой».",
                style = MaterialTheme.typography.bodySmall
            )
        }

        item { SectionTitle("Фазный ток") }
        item {
            AmpSliderCard(
                value = state.tuningPhaseCurrentAmps,
                min = TuningLimits.PHASE_CURRENT_MIN_A,
                max = TuningLimits.PHASE_CURRENT_MAX_A,
                onChange = onSetPhaseCurrent
            )
        }

        item { SectionTitle("Батарейный ток") }
        item {
            AmpSliderCard(
                value = state.tuningBatteryCurrentAmps,
                min = TuningLimits.BATTERY_CURRENT_MIN_A,
                max = TuningLimits.BATTERY_CURRENT_MAX_A,
                onChange = onSetBatteryCurrent
            )
        }

        item { SectionTitle("Ток рекуперации") }
        item {
            AmpSliderCard(
                value = state.tuningRegenCurrentAmps,
                min = TuningLimits.REGEN_CURRENT_MIN_A,
                max = TuningLimits.REGEN_CURRENT_MAX_A,
                onChange = onSetRegenCurrent
            )
        }

        item { SectionTitle("Override максимальной скорости") }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "${state.tuningSpeedLimitOverrideKmh} км/ч")
                    Slider(
                        value = state.tuningSpeedLimitOverrideKmh.toFloat(),
                        onValueChange = { onSetSpeedLimitOverride(it.toInt()) },
                        valueRange = TuningLimits.SPEED_MIN_KMH.toFloat()..TuningLimits.SPEED_MAX_KMH.toFloat(),
                        steps = TuningLimits.SPEED_MAX_KMH - TuningLimits.SPEED_MIN_KMH - 1
                    )
                }
            }
        }

        item { SectionTitle("Брейк буст") }
        item {
            PercentSliderCard(value = state.tuningBrakeBoostPercent, onChange = onSetBrakeBoost)
        }

        item { SectionTitle("Буст на газ") }
        item {
            PercentSliderCard(value = state.tuningThrottleBoostPercent, onChange = onSetThrottleBoost)
        }

        item { SectionTitle("Буст на разгон") }
        item {
            PercentSliderCard(value = state.tuningAccelBoostPercent, onChange = onSetAccelBoost)
        }

        item { SectionTitle("Field weakening") }
        item {
            ToggleCard(
                title = if (state.tuningFieldWeakeningEnabled) "Включено" else "Выключено",
                subtitle = "Повышает верхнюю скорость за счёт КПД и нагрева мотора",
                checked = state.tuningFieldWeakeningEnabled,
                onCheckedChange = onSetFieldWeakening
            )
        }

        item { SectionTitle("Круиз-контроль") }
        item {
            ToggleCard(
                title = if (state.cruiseControlEnabled) "Включён" else "Выключен",
                subtitle = "Удерживает текущую скорость без удержания газа",
                checked = state.cruiseControlEnabled,
                onCheckedChange = onSetCruiseControl
            )
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text = text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
}

@Composable
private fun AmpSliderCard(value: Int, min: Int, max: Int, onChange: (Int) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            val warn = value > max * 0.6
            Text(
                text = "$value А",
                color = if (warn) BrandOrange else MaterialTheme.colorScheme.onSurface
            )
            Slider(
                value = value.toFloat(),
                onValueChange = { onChange(it.toInt()) },
                valueRange = min.toFloat()..max.toFloat(),
                steps = max - min - 1
            )
        }
    }
}

@Composable
private fun PercentSliderCard(value: Int, onChange: (Int) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "$value %")
            Slider(
                value = value.toFloat(),
                onValueChange = { onChange(it.toInt()) },
                valueRange = TuningLimits.BOOST_MIN_PERCENT.toFloat()..TuningLimits.BOOST_MAX_PERCENT.toFloat(),
                steps = TuningLimits.BOOST_MAX_PERCENT - TuningLimits.BOOST_MIN_PERCENT - 1
            )
        }
    }
}

@Composable
private fun ToggleCard(title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(text = title)
                Text(text = subtitle, style = MaterialTheme.typography.bodySmall)
            }
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}
