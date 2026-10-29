package com.ninebot.control.demo

import com.ninebot.control.ble.ConnectionStatus
import com.ninebot.control.ble.ScooterState
import com.ninebot.control.ble.SpeedMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

/**
 * Генерирует правдоподобные показания самоката, чтобы можно было посмотреть
 * весь функционал приложения до подключения к реальному устройству.
 */
class DemoSimulator(private val scope: CoroutineScope) {

    private val _state = MutableStateFlow(
        ScooterState(
            connection = ConnectionStatus.CONNECTED,
            deviceName = "DEMO Ninebot Max Plus",
            speedKmh = 0.0,
            powerWatts = 0,
            batteryPercent = 78,
            batteryVoltage = 41.2,
            batteryTemperatureC = 24,
            tripDistanceKm = 0.0,
            totalDistanceKm = 142.6,
            speedMode = SpeedMode.MODE_2,
            movementLocked = false
        )
    )
    val state: StateFlow<ScooterState> = _state

    private var running = false

    fun start() {
        if (running) return
        running = true
        scope.launch {
            var targetSpeed = 0.0
            while (running) {
                delay(400)
                val s = _state.value
                if (s.movementLocked) {
                    targetSpeed = 0.0
                } else {
                    val modeCap = when (s.speedMode) {
                        SpeedMode.MODE_1 -> 6.0
                        SpeedMode.MODE_2 -> 20.0
                        SpeedMode.MODE_3 -> 30.0
                    }
                    // На кастомной прошивке override может поднимать лимит выше заводского.
                    val cap = max(modeCap, s.tuningSpeedLimitOverrideKmh.toDouble())
                    targetSpeed = (targetSpeed + Random.nextDouble(-2.0, 3.0)).coerceIn(0.0, cap)
                }
                val speed = targetSpeed
                val currentCapFactor = s.tuningCurrentLimitAmps / 15.0
                val power = (speed * Random.nextDouble(20.0, 32.0) * currentCapFactor).toInt()
                val battery = max(0, s.batteryPercent - if (Random.nextInt(40) == 0) 1 else 0)
                val trip = s.tripDistanceKm + speed * (0.4 / 3600.0)
                _state.update {
                    it.copy(
                        speedKmh = speed,
                        powerWatts = power,
                        batteryPercent = battery,
                        batteryTemperatureC = (24 + (power / 150)).coerceIn(18, 45),
                        tripDistanceKm = trip,
                        totalDistanceKm = it.totalDistanceKm + speed * (0.4 / 3600.0)
                    )
                }
            }
        }
    }

    fun stop() {
        running = false
    }

    fun setSpeedMode(mode: SpeedMode) {
        _state.update { it.copy(speedMode = mode) }
    }

    fun setMovementLock(locked: Boolean) {
        _state.update { it.copy(movementLocked = locked, speedKmh = if (locked) 0.0 else it.speedKmh) }
    }

    fun setCurrentLimit(amps: Int) {
        _state.update { it.copy(tuningCurrentLimitAmps = amps) }
    }

    fun setSpeedLimitOverride(kmh: Int) {
        _state.update { it.copy(tuningSpeedLimitOverrideKmh = kmh) }
    }

    fun setFieldWeakening(enabled: Boolean) {
        _state.update { it.copy(tuningFieldWeakeningEnabled = enabled) }
    }
}
