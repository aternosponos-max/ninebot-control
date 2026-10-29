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
    val tuningCurrentLimitAmps: Int = 15,
    val tuningSpeedLimitOverrideKmh: Int = 25,
    val tuningFieldWeakeningEnabled: Boolean = false,
    val cruiseControlEnabled: Boolean = false,
    val regenBrakingLevel: Int = 1,

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

/** Границы параметров тюнинга — используются UI как min/max слайдеров. */
object TuningLimits {
    const val CURRENT_MIN_A = 5
    const val CURRENT_MAX_A = 45
    const val SPEED_MIN_KMH = 6
    const val SPEED_MAX_KMH = 55
    const val REGEN_MIN = 0
    const val REGEN_MAX = 3
}

/** Категории для карточек приборной панели. */
enum class MetricCategory(val title: String) {
    SPEED("Скорость"),
    POWER("Мощность"),
    BATTERY("Батарея"),
    TRIP("Пробег")
}
