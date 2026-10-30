package com.ninebot.control.ble

/** Режимы ограничения скорости. Соответствие "передачам" 1/2/3 из задания. */
enum class SpeedMode(val label: String, val approxKmh: String) {
    MODE_1("1 — Эко", "до ~6 км/ч"),
    MODE_2("2 — Драйв", "до ~20 км/ч"),
    MODE_3("3 — Спорт", "до ~30 км/ч")
}

enum class ConnectionStatus {
    DISCONNECTED, SCANNING, CONNECTING, CONNECTED, ERROR
}

/** Профили прошивки — набор пресетов параметров тюнинга. */
enum class TuningProfile(val label: String) {
    ECO("Эко"),
    COMFORT("Комфорт"),
    SPORT("Спорт"),
    CUSTOM("Свой")
}

/** Значения параметров, которые применяет пресет профиля. */
data class TuningPreset(
    val phaseCurrentAmps: Int,
    val batteryCurrentAmps: Int,
    val speedLimitOverrideKmh: Int,
    val regenCurrentAmps: Int,
    val fieldWeakeningEnabled: Boolean
)

object TuningPresets {
    val PRESETS: Map<TuningProfile, TuningPreset> = mapOf(
        TuningProfile.ECO to TuningPreset(
            phaseCurrentAmps = 20, batteryCurrentAmps = 12, speedLimitOverrideKmh = 20,
            regenCurrentAmps = 6, fieldWeakeningEnabled = false
        ),
        TuningProfile.COMFORT to TuningPreset(
            phaseCurrentAmps = 35, batteryCurrentAmps = 20, speedLimitOverrideKmh = 30,
            regenCurrentAmps = 10, fieldWeakeningEnabled = false
        ),
        TuningProfile.SPORT to TuningPreset(
            phaseCurrentAmps = 60, batteryCurrentAmps = 35, speedLimitOverrideKmh = 45,
            regenCurrentAmps = 15, fieldWeakeningEnabled = true
        )
    )
}

/**
 * Снимок телеметрии самоката. В демо-режиме заполняется симулятором,
 * в боевом режиме — парсером пакетов [NinebotProtocol].
 */
data class ScooterState(
    val connection: ConnectionStatus = ConnectionStatus.DISCONNECTED,
    val deviceName: String? = null,
    val speedKmh: Double = 0.0,
    val powerWatts: Int = 0,
    val batteryPercent: Int = 0,
    val batteryVoltage: Double = 0.0,
    val batteryTemperatureC: Int = 0,
    val tripDistanceKm: Double = 0.0,
    val totalDistanceKm: Double = 0.0,
    val speedMode: SpeedMode = SpeedMode.MODE_2,
    val movementLocked: Boolean = false,
    val lastRawPacketHex: String = "",
    val log: List<String> = emptyList(),

    // Параметры доступны только на самокатах с кастомной прошивкой (DRV и
    // подобные), дающей доступ к этим регистрам. На заводской/шеринговой
    // прошивке соответствующие пакеты просто не будут поняты контроллером.
    // Верхние границы (120 А) заданы по явному запросу пользователя для
    // личного контроллера с усиленной силовой частью — см. предупреждение
    // на вкладке "Тюнинг".
    val tuningPhaseCurrentAmps: Int = 30,
    val tuningBatteryCurrentAmps: Int = 20,
    val tuningSpeedLimitOverrideKmh: Int = 25,
    val tuningFieldWeakeningEnabled: Boolean = false,
    val cruiseControlEnabled: Boolean = false,
    val tuningRegenCurrentAmps: Int = 8,
    val tuningBrakeBoostPercent: Int = 0,
    val tuningThrottleBoostPercent: Int = 0,
    val tuningAccelBoostPercent: Int = 0,
    val tuningProfile: TuningProfile = TuningProfile.CUSTOM,

    // Данные для вкладки "Статистика" — аналог функций из сторонних
    // дашборд-приложений (Ninedash и подобные): детальная диагностика
    // батареи, ошибки контроллера, версия прошивки, статистика поездки.
    val cellVoltages: List<Double> = emptyList(),
    val errorCodes: List<String> = emptyList(),
    val firmwareVersion: String = "—",
    val serialNumber: String = "—",
    val rideTimeSeconds: Long = 0,
    val maxSpeedSessionKmh: Double = 0.0,
    val avgSpeedSessionKmh: Double = 0.0
)

/**
 * Границы параметров тюнинга — используются UI как min/max слайдеров.
 *
 * Верхние пределы тока (120 А по фазе и по батарее) заданы по прямому
 * запросу пользователя для личного самоката с усиленным контроллером —
 * это сильно выше типичных значений даже для кастомных прошивок серии
 * M365/Ninebot (там реальный потолок у сообщества обычно в районе 40–60 А
 * даже на доработанном железе). Ответственность за соответствие этих
 * значений реальным возможностям конкретного контроллера лежит на
 * владельце устройства.
 */
object TuningLimits {
    const val PHASE_CURRENT_MIN_A = 10
    const val PHASE_CURRENT_MAX_A = 120
    const val BATTERY_CURRENT_MIN_A = 5
    const val BATTERY_CURRENT_MAX_A = 120
    const val REGEN_CURRENT_MIN_A = 0
    const val REGEN_CURRENT_MAX_A = 120
    const val SPEED_MIN_KMH = 6
    const val SPEED_MAX_KMH = 55
    const val BOOST_MIN_PERCENT = 0
    const val BOOST_MAX_PERCENT = 100
}

/** Категории для карточек приборной панели. */
enum class MetricCategory(val title: String) {
    SPEED("Скорость"),
    POWER("Мощность"),
    BATTERY("Батарея"),
    TRIP("Пробег")
}
