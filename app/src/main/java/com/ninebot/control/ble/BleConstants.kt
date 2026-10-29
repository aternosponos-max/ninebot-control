package com.ninebot.control.ble

import java.util.UUID

/**
 * ВНИМАНИЕ: UUID и коды команд ниже собраны по открытым материалам сообщества
 * о протоколе Ninebot/Segway BLE (общая схема с протоколом Xiaomi M365/ES/Max:
 * заголовок 0x5A 0xA5, адреса источника/назначения, код команды, регистр,
 * полезная нагрузка, 16-битная контрольная сумма). Для конкретной прошивки
 * SNSC значения регистров НЕ подтверждены и должны быть проверены вручную —
 * используйте экран "Журнал" в приложении, чтобы сверить реальные пакеты
 * перед тем как полагаться на отправку команд.
 */
object BleConstants {
    // Nordic UART-подобный сервис, который часто используется BLE-модулями Ninebot.
    val SERVICE_UUID: UUID = UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e")
    val WRITE_CHARACTERISTIC_UUID: UUID = UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dcca9e")
    val NOTIFY_CHARACTERISTIC_UUID: UUID = UUID.fromString("6e400003-b5a3-f393-e0a9-e50e24dcca9e")
    val CLIENT_CHARACTERISTIC_CONFIG_UUID: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

    const val SCAN_TIMEOUT_MS = 15_000L
}
