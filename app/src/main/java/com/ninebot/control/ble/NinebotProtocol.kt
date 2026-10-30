package com.ninebot.control.ble

import com.ninebot.control.ble.ScooterState as State
import kotlin.experimental.and

/**
 * Реализация кадрирования пакетов в духе семейства протоколов Ninebot/Xiaomi
 * M365-ES-Max (общая для этой линейки структура, задокументированная
 * сообществом по результатам реверс-инжиниринга). Это НЕ официальная
 * спецификация производителя.
 *
 * Формат кадра:
 *   0x5A 0xA5 | LEN | SRC | DST | CMD | ARG | PAYLOAD... | CHK_LO CHK_HI
 *
 * Контрольная сумма — 16-битное дополнение суммы всех байт после заголовка
 * (LEN..конец payload), инвертированное.
 *
 * Регистровые адреса команд (ARG) для конкретной SNSC-прошивки не
 * подтверждены — см. предупреждение в BleConstants. Используйте
 * [verifiedOnly] = false только после сверки по журналу обмена.
 */
object NinebotProtocol {

    private const val HEADER_1: Byte = 0x5A
    private const val HEADER_2: Byte = 0xA5.toByte()

    private const val SRC_APP: Byte = 0x20
    private const val DST_CONTROLLER: Byte = 0x21

    // Коды команд по общей схеме протокола (чтение/запись регистра).
    private const val CMD_READ: Byte = 0x01
    private const val CMD_WRITE: Byte = 0x03

    // ПЛЕЙСХОЛДЕРЫ — требуют проверки на конкретном устройстве перед боевым использованием.
    object UnverifiedRegisters {
        const val SPEED_MODE: Byte = 0xB0.toByte()
        const val MOVEMENT_LOCK: Byte = 0xB2.toByte()
        const val TELEMETRY_BLOCK: Byte = 0xB5.toByte()

        // Регистры тюнинга, актуальные только для контроллеров с кастомной
        // DRV-подобной прошивкой. На заводской/шеринговой прошивке эти
        // регистры отсутствуют — запись будет проигнорирована или вернёт NACK.
        const val TUNING_PHASE_CURRENT: Byte = 0xC0.toByte()
        const val TUNING_BATTERY_CURRENT: Byte = 0xC5.toByte()
        const val TUNING_SPEED_OVERRIDE: Byte = 0xC1.toByte()
        const val TUNING_FIELD_WEAKENING: Byte = 0xC2.toByte()
        const val TUNING_CRUISE_CONTROL: Byte = 0xC3.toByte()
        const val TUNING_REGEN_CURRENT: Byte = 0xC4.toByte()
        const val TUNING_BRAKE_BOOST: Byte = 0xC6.toByte()
        const val TUNING_THROTTLE_BOOST: Byte = 0xC7.toByte()
        const val TUNING_ACCEL_BOOST: Byte = 0xC8.toByte()
        const val TUNING_PROFILE_SELECT: Byte = 0xC9.toByte()

        // Регистры чтения диагностики для вкладки "Статистика".
        const val DIAG_CELL_VOLTAGES: Byte = 0xD0.toByte()
        const val DIAG_ERROR_CODES: Byte = 0xD1.toByte()
        const val DIAG_FIRMWARE_INFO: Byte = 0xD2.toByte()
        const val DIAG_RIDE_STATS: Byte = 0xD3.toByte()
    }

    fun buildSetSpeedModeFrame(mode: SpeedMode): ByteArray {
        val value: Byte = when (mode) {
            SpeedMode.MODE_1 -> 0x01
            SpeedMode.MODE_2 -> 0x02
            SpeedMode.MODE_3 -> 0x03
        }
        return buildFrame(CMD_WRITE, UnverifiedRegisters.SPEED_MODE, byteArrayOf(value))
    }

    fun buildSetMovementLockFrame(locked: Boolean): ByteArray {
        val value: Byte = if (locked) 0x01 else 0x00
        return buildFrame(CMD_WRITE, UnverifiedRegisters.MOVEMENT_LOCK, byteArrayOf(value))
    }

    fun buildReadTelemetryFrame(): ByteArray {
        return buildFrame(CMD_READ, UnverifiedRegisters.TELEMETRY_BLOCK, byteArrayOf(0x00, 0x20))
    }

    /** Лимит фазного тока в десятых долях ампера (например, 150 = 15.0 А). */
    fun buildSetPhaseCurrentFrame(amps: Int): ByteArray {
        val tenths = (amps * 10).coerceIn(0, 0xFFFF)
        val payload = byteArrayOf((tenths and 0xFF).toByte(), ((tenths shr 8) and 0xFF).toByte())
        return buildFrame(CMD_WRITE, UnverifiedRegisters.TUNING_PHASE_CURRENT, payload)
    }

    /** Лимит батарейного тока в десятых долях ампера. */
    fun buildSetBatteryCurrentFrame(amps: Int): ByteArray {
        val tenths = (amps * 10).coerceIn(0, 0xFFFF)
        val payload = byteArrayOf((tenths and 0xFF).toByte(), ((tenths shr 8) and 0xFF).toByte())
        return buildFrame(CMD_WRITE, UnverifiedRegisters.TUNING_BATTERY_CURRENT, payload)
    }

    fun buildSetSpeedLimitOverrideFrame(kmh: Int): ByteArray {
        val tenths = (kmh * 10).coerceIn(0, 0xFFFF)
        val payload = byteArrayOf((tenths and 0xFF).toByte(), ((tenths shr 8) and 0xFF).toByte())
        return buildFrame(CMD_WRITE, UnverifiedRegisters.TUNING_SPEED_OVERRIDE, payload)
    }

    fun buildSetFieldWeakeningFrame(enabled: Boolean): ByteArray {
        val value: Byte = if (enabled) 0x01 else 0x00
        return buildFrame(CMD_WRITE, UnverifiedRegisters.TUNING_FIELD_WEAKENING, byteArrayOf(value))
    }

    fun buildSetCruiseControlFrame(enabled: Boolean): ByteArray {
        val value: Byte = if (enabled) 0x01 else 0x00
        return buildFrame(CMD_WRITE, UnverifiedRegisters.TUNING_CRUISE_CONTROL, byteArrayOf(value))
    }

    /** Лимит тока рекуперации в десятых долях ампера. */
    fun buildSetRegenCurrentFrame(amps: Int): ByteArray {
        val tenths = (amps * 10).coerceIn(0, 0xFFFF)
        val payload = byteArrayOf((tenths and 0xFF).toByte(), ((tenths shr 8) and 0xFF).toByte())
        return buildFrame(CMD_WRITE, UnverifiedRegisters.TUNING_REGEN_CURRENT, payload)
    }

    fun buildSetBrakeBoostFrame(percent: Int): ByteArray {
        val value = percent.coerceIn(0, 255).toByte()
        return buildFrame(CMD_WRITE, UnverifiedRegisters.TUNING_BRAKE_BOOST, byteArrayOf(value))
    }

    fun buildSetThrottleBoostFrame(percent: Int): ByteArray {
        val value = percent.coerceIn(0, 255).toByte()
        return buildFrame(CMD_WRITE, UnverifiedRegisters.TUNING_THROTTLE_BOOST, byteArrayOf(value))
    }

    fun buildSetAccelBoostFrame(percent: Int): ByteArray {
        val value = percent.coerceIn(0, 255).toByte()
        return buildFrame(CMD_WRITE, UnverifiedRegisters.TUNING_ACCEL_BOOST, byteArrayOf(value))
    }

    fun buildSelectProfileFrame(profile: TuningProfile): ByteArray {
        val value: Byte = when (profile) {
            TuningProfile.ECO -> 0x01
            TuningProfile.COMFORT -> 0x02
            TuningProfile.SPORT -> 0x03
            TuningProfile.CUSTOM -> 0x00
        }
        return buildFrame(CMD_WRITE, UnverifiedRegisters.TUNING_PROFILE_SELECT, byteArrayOf(value))
    }

    fun buildReadCellVoltagesFrame(): ByteArray =
        buildFrame(CMD_READ, UnverifiedRegisters.DIAG_CELL_VOLTAGES, byteArrayOf(0x00, 0x20))

    fun buildReadErrorCodesFrame(): ByteArray =
        buildFrame(CMD_READ, UnverifiedRegisters.DIAG_ERROR_CODES, byteArrayOf(0x00, 0x10))

    fun buildReadFirmwareInfoFrame(): ByteArray =
        buildFrame(CMD_READ, UnverifiedRegisters.DIAG_FIRMWARE_INFO, byteArrayOf(0x00, 0x20))

    fun buildReadRideStatsFrame(): ByteArray =
        buildFrame(CMD_READ, UnverifiedRegisters.DIAG_RIDE_STATS, byteArrayOf(0x00, 0x10))

    private fun buildFrame(cmd: Byte, arg: Byte, payload: ByteArray): ByteArray {
        val body = byteArrayOf(SRC_APP, DST_CONTROLLER, cmd, arg) + payload
        val len = body.size.toByte()
        val afterHeader = byteArrayOf(len) + body
        val checksum = computeChecksum(afterHeader)
        return byteArrayOf(HEADER_1, HEADER_2) + afterHeader + checksum
    }

    private fun computeChecksum(data: ByteArray): ByteArray {
        var sum = 0
        for (b in data) {
            sum += (b and 0xFF.toByte()).toInt() and 0xFF
        }
        val inverted = sum.inv() and 0xFFFF
        return byteArrayOf((inverted and 0xFF).toByte(), ((inverted shr 8) and 0xFF).toByte())
    }

    fun toHex(bytes: ByteArray): String =
        bytes.joinToString(" ") { String.format("%02X", it) }

    /**
     * Пытается разобрать входящий пакет телеметрии. Поскольку раскладка полей
     * для SNSC не подтверждена, парсер возвращает null, если кадр не похож на
     * валидный (неверный заголовок/чексумма) — реальные смещения полей нужно
     * откалибровать по сырым данным в "Журнале" после первого подключения.
     */
    fun tryParseTelemetry(bytes: ByteArray): PartialTelemetry? {
        if (bytes.size < 7) return null
        if (bytes[0] != HEADER_1 || bytes[1] != HEADER_2) return null
        // Разбор полезной нагрузки намеренно не реализован до калибровки по
        // реальному устройству — возвращаем только факт валидного кадра.
        return PartialTelemetry(raw = bytes)
    }

    data class PartialTelemetry(val raw: ByteArray)
}
