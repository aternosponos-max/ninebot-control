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
import com.ninebot.control.ui.theme.BrandOrange

/**
 * Расширенные параметры тюнинга (аналог функций DRV и подобных кастомных
 * прошивок): лимит тока, override максимальной скорости, field weakening.
 *
 * Работает только с самокатами, где уже установлена совместимая кастомная
 * прошивка контроллера — заводская/шеринговая прошивка эти регистры не
 * поддерживает, запись будет проигнорирована контроллером.
 */
@Composable
fun TuningScreen(
    state: ScooterState,
    onSetCurrentLimit: (Int) -> Unit,
    onSetSpeedLimitOverride: (Int) -> Unit,
    onSetFieldWeakening: (Boolean) -> Unit,
    onSetCruiseControl: (Boolean) -> Unit,
    onSetRegenBraking: (Int) -> Unit
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
                        tint = BrandOrange
                    )
                    Text(
                        text = "  Эти параметры действуют только на контроллерах с установленной " +
                            "совместимой кастомной прошивкой (DRV и аналоги). На заводской или " +
                            "шеринговой прошивке пакеты будут проигнорированы — самокат просто не " +
                            "ответит на эти регистры. Завышенный ток повышает нагрузку на " +
                            "контроллер, мотор и батарею — меняйте значения постепенно и следите " +
                            "за температурой.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        item {
            Text(
                text = "Лимит тока",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "${state.tuningCurrentLimitAmps} А")
                    Slider(
                        value = state.tuningCurrentLimitAmps.toFloat(),
                        onValueChange = { onSetCurrentLimit(it.toInt()) },
                        valueRange = TuningLimits.CURRENT_MIN_A.toFloat()..TuningLimits.CURRENT_MAX_A.toFloat(),
                        steps = TuningLimits.CURRENT_MAX_A - TuningLimits.CURRENT_MIN_A - 1
                    )
                }
            }
        }

        item {
            Text(
                text = "Override максимальной скорости",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
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

        item {
            Text(
                text = "Field weakening",
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
                        Text(text = if (state.tuningFieldWeakeningEnabled) "Включено" else "Выключено")
                        Text(
                            text = "Повышает верхнюю скорость за счёт КПД и нагрева мотора",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Switch(
                        checked = state.tuningFieldWeakeningEnabled,
                        onCheckedChange = onSetFieldWeakening
                    )
                }
            }
        }

        item {
            Text(
                text = "Круиз-контроль",
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
                        Text(text = if (state.cruiseControlEnabled) "Включён" else "Выключен")
                        Text(
                            text = "Удерживает текущую скорость без удержания газа",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Switch(
                        checked = state.cruiseControlEnabled,
                        onCheckedChange = onSetCruiseControl
                    )
                }
            }
        }

        item {
            Text(
                text = "Рекуперативное торможение",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "Уровень ${state.regenBrakingLevel} из ${TuningLimits.REGEN_MAX}")
                    Slider(
                        value = state.regenBrakingLevel.toFloat(),
                        onValueChange = { onSetRegenBraking(it.toInt()) },
                        valueRange = TuningLimits.REGEN_MIN.toFloat()..TuningLimits.REGEN_MAX.toFloat(),
                        steps = TuningLimits.REGEN_MAX - TuningLimits.REGEN_MIN - 1
                    )
                    Text(
                        text = "Выше — сильнее торможение двигателем и быстрее подзаряд батареи при спуске",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}
