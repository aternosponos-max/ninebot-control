package com.ninebot.control.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.os.Build
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

/**
 * Управляет сканированием, подключением и обменом GATT-пакетами с самокатом.
 * Все операции best-effort: часть регистров протокола не подтверждена
 * (см. NinebotProtocol), поэтому менеджер только отправляет кадры и логирует
 * сырой обмен — интерпретация ответов требует ручной проверки пользователем.
 */
@SuppressLint("MissingPermission")
class NinebotBleManager(private val context: Context) {

    private val bluetoothManager =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val adapter: BluetoothAdapter? = bluetoothManager.adapter

    private var gatt: BluetoothGatt? = null
    private var writeCharacteristic: BluetoothGattCharacteristic? = null

    private val _state = MutableStateFlow(ScooterState())
    val state: StateFlow<ScooterState> = _state

    private val _discoveredDevices = MutableStateFlow<List<BluetoothDevice>>(emptyList())
    val discoveredDevices: StateFlow<List<BluetoothDevice>> = _discoveredDevices

    fun isBluetoothEnabled(): Boolean = adapter?.isEnabled == true

    fun startScan() {
        val scanner = adapter?.bluetoothLeScanner ?: return
        _discoveredDevices.value = emptyList()
        _state.update { it.copy(connection = ConnectionStatus.SCANNING) }
        scanner.startScan(scanCallback)
    }

    fun stopScan() {
        adapter?.bluetoothLeScanner?.stopScan(scanCallback)
        if (_state.value.connection == ConnectionStatus.SCANNING) {
            _state.update { it.copy(connection = ConnectionStatus.DISCONNECTED) }
        }
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device
            val current = _discoveredDevices.value
            if (current.none { it.address == device.address }) {
                _discoveredDevices.value = current + device
            }
        }

        override fun onScanFailed(errorCode: Int) {
            appendLog("Сканирование не удалось, код $errorCode")
            _state.update { it.copy(connection = ConnectionStatus.ERROR) }
        }
    }

    fun connect(device: BluetoothDevice) {
        stopScan()
        _state.update { it.copy(connection = ConnectionStatus.CONNECTING, deviceName = device.name ?: device.address) }
        gatt = device.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
    }

    fun disconnect() {
        gatt?.disconnect()
        gatt?.close()
        gatt = null
        writeCharacteristic = null
        _state.update { it.copy(connection = ConnectionStatus.DISCONNECTED) }
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(g: BluetoothGatt, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    appendLog("GATT подключён, ищем сервисы…")
                    g.discoverServices()
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    appendLog("GATT отключён")
                    _state.update { it.copy(connection = ConnectionStatus.DISCONNECTED) }
                }
            }
        }

        override fun onServicesDiscovered(g: BluetoothGatt, status: Int) {
            val service = g.getService(BleConstants.SERVICE_UUID)
            if (service == null) {
                appendLog("Ожидаемый сервис ${BleConstants.SERVICE_UUID} не найден. Проверьте UUID вручную.")
                _state.update { it.copy(connection = ConnectionStatus.ERROR) }
                return
            }
            writeCharacteristic = service.getCharacteristic(BleConstants.WRITE_CHARACTERISTIC_UUID)
            val notifyChar = service.getCharacteristic(BleConstants.NOTIFY_CHARACTERISTIC_UUID)
            if (notifyChar != null) {
                g.setCharacteristicNotification(notifyChar, true)
                val cccd = notifyChar.getDescriptor(BleConstants.CLIENT_CHARACTERISTIC_CONFIG_UUID)
                if (cccd != null) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        g.writeDescriptor(cccd, BluetoothGattDescriptorCompatEnableNotify)
                    } else {
                        @Suppress("DEPRECATION")
                        cccd.value = BluetoothGattDescriptorCompatEnableNotify
                        @Suppress("DEPRECATION")
                        g.writeDescriptor(cccd)
                    }
                }
            }
            _state.update { it.copy(connection = ConnectionStatus.CONNECTED) }
            appendLog("Подключено. Сервис и характеристики найдены.")
        }

        override fun onCharacteristicChanged(
            g: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            val hex = NinebotProtocol.toHex(value)
            appendLog("RX: $hex")
            _state.update { it.copy(lastRawPacketHex = hex) }
            NinebotProtocol.tryParseTelemetry(value)
            // Разбор полей телеметрии не реализован до калибровки по реальному
            // устройству — см. NinebotProtocol.tryParseTelemetry.
        }
    }

    fun sendSetSpeedMode(mode: SpeedMode) {
        val frame = NinebotProtocol.buildSetSpeedModeFrame(mode)
        writeFrame(frame)
        _state.update { it.copy(speedMode = mode) }
    }

    fun sendSetMovementLock(locked: Boolean) {
        appendLog("Команда блокировки движения отключена: для SNSC 2.3 не подтверждена. Это не замок батарейной деки.")
    }

    fun requestTelemetry() {
        writeFrame(NinebotProtocol.buildReadTelemetryFrame())
    }

    fun sendSetPhaseCurrent(amps: Int) {
        if (!NinebotProtocol.supportsShfwTuning) { appendLog("SHFW: запись фазного тока отключена — публичный протокол команды не подтверждён."); return }
        writeFrame(NinebotProtocol.buildSetPhaseCurrentFrame(amps))
        _state.update { it.copy(tuningPhaseCurrentAmps = amps, tuningProfile = TuningProfile.CUSTOM) }
    }

    fun sendSetBatteryCurrent(amps: Int) {
        if (!NinebotProtocol.supportsShfwTuning) { appendLog("SHFW: запись батарейного тока отключена — публичный протокол команды не подтверждён."); return }
        writeFrame(NinebotProtocol.buildSetBatteryCurrentFrame(amps))
        _state.update { it.copy(tuningBatteryCurrentAmps = amps, tuningProfile = TuningProfile.CUSTOM) }
    }

    fun sendSetSpeedLimitOverride(kmh: Int) {
        if (!NinebotProtocol.supportsShfwTuning) { appendLog("SHFW: запись лимита скорости отключена — публичный протокол команды не подтверждён."); return }
        writeFrame(NinebotProtocol.buildSetSpeedLimitOverrideFrame(kmh))
        _state.update { it.copy(tuningSpeedLimitOverrideKmh = kmh) }
    }

    fun sendSetFieldWeakening(enabled: Boolean) {
        if (!NinebotProtocol.supportsShfwTuning) { appendLog("SHFW: запись ослабления поля отключена — публичный протокол команды не подтверждён."); return }
        writeFrame(NinebotProtocol.buildSetFieldWeakeningFrame(enabled))
        _state.update { it.copy(tuningFieldWeakeningEnabled = enabled) }
    }

    fun sendSetCruiseControl(enabled: Boolean) {
        if (!NinebotProtocol.supportsShfwTuning) { appendLog("SHFW: запись круиз-контроля отключена — сначала нужно проверить поддержку этой прошивкой."); return }
        writeFrame(NinebotProtocol.buildSetCruiseControlFrame(enabled))
        _state.update { it.copy(cruiseControlEnabled = enabled) }
    }

    fun sendSetRegenCurrent(amps: Int) {
        if (!NinebotProtocol.supportsShfwTuning) { appendLog("SHFW: запись тока рекуперации отключена — публичный протокол команды не подтверждён."); return }
        writeFrame(NinebotProtocol.buildSetRegenCurrentFrame(amps))
        _state.update { it.copy(tuningRegenCurrentAmps = amps, tuningProfile = TuningProfile.CUSTOM) }
    }

    fun sendSetBrakeBoost(percent: Int) {
        if (!NinebotProtocol.supportsShfwTuning) { appendLog("SHFW: Brake Boost отключён — публичный протокол команды не подтверждён."); return }
        writeFrame(NinebotProtocol.buildSetBrakeBoostFrame(percent))
        _state.update { it.copy(tuningBrakeBoostPercent = percent) }
    }

    fun sendSetThrottleBoost(percent: Int) {
        if (!NinebotProtocol.supportsShfwTuning) { appendLog("SHFW: Throttle Boost отключён — публичный протокол команды не подтверждён."); return }
        writeFrame(NinebotProtocol.buildSetThrottleBoostFrame(percent))
        _state.update { it.copy(tuningThrottleBoostPercent = percent) }
    }

    fun sendSetAccelBoost(percent: Int) {
        if (!NinebotProtocol.supportsShfwTuning) { appendLog("SHFW: Acceleration Boost отключён — публичный протокол команды не подтверждён."); return }
        writeFrame(NinebotProtocol.buildSetAccelBoostFrame(percent))
        _state.update { it.copy(tuningAccelBoostPercent = percent) }
    }

    fun sendSelectProfile(profile: TuningProfile) {
        if (!NinebotProtocol.supportsShfwTuning) { appendLog("SHFW-профили отключены: нет подтверждённого протокола записи настроек."); return }
        writeFrame(NinebotProtocol.buildSelectProfileFrame(profile))
        if (profile == TuningProfile.CUSTOM) {
            _state.update { it.copy(tuningProfile = profile) }
            return
        }
        val preset = TuningPresets.PRESETS[profile] ?: return
        // Применяем пресет локально и рассылаем его как набор обычных команд,
        // чтобы контроллер получил те же регистры, что и при ручной настройке.
        writeFrame(NinebotProtocol.buildSetPhaseCurrentFrame(preset.phaseCurrentAmps))
        writeFrame(NinebotProtocol.buildSetBatteryCurrentFrame(preset.batteryCurrentAmps))
        writeFrame(NinebotProtocol.buildSetSpeedLimitOverrideFrame(preset.speedLimitOverrideKmh))
        writeFrame(NinebotProtocol.buildSetRegenCurrentFrame(preset.regenCurrentAmps))
        writeFrame(NinebotProtocol.buildSetFieldWeakeningFrame(preset.fieldWeakeningEnabled))
        _state.update {
            it.copy(
                tuningProfile = profile,
                tuningPhaseCurrentAmps = preset.phaseCurrentAmps,
                tuningBatteryCurrentAmps = preset.batteryCurrentAmps,
                tuningSpeedLimitOverrideKmh = preset.speedLimitOverrideKmh,
                tuningRegenCurrentAmps = preset.regenCurrentAmps,
                tuningFieldWeakeningEnabled = preset.fieldWeakeningEnabled
            )
        }
    }

    /**
     * Запрашивает диагностический блок (ячейки батареи, ошибки, прошивка,
     * статистика поездки). Разбор ответа не реализован до калибровки по
     * реальному устройству — см. NinebotProtocol.tryParseTelemetry. Сырой
     * ответ будет виден в журнале на вкладке "Подключение".
     */
    fun requestDiagnostics() {
        writeFrame(NinebotProtocol.buildReadCellVoltagesFrame())
        writeFrame(NinebotProtocol.buildReadErrorCodesFrame())
        writeFrame(NinebotProtocol.buildReadFirmwareInfoFrame())
        writeFrame(NinebotProtocol.buildReadRideStatsFrame())
    }

    private fun writeFrame(frame: ByteArray) {
        if (frame.isEmpty()) {
            appendLog("Команда не отправлена: для этой функции нет подтверждённого кадра протокола.")
            return
        }
        val g = gatt ?: return
        val char = writeCharacteristic ?: return
        appendLog("TX: ${NinebotProtocol.toHex(frame)}")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            g.writeCharacteristic(char, frame, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT)
        } else {
            @Suppress("DEPRECATION")
            char.value = frame
            @Suppress("DEPRECATION")
            g.writeCharacteristic(char)
        }
    }

    private fun appendLog(line: String) {
        _state.update { it.copy(log = (it.log + line).takeLast(200)) }
    }

    companion object {
        private val BluetoothGattDescriptorCompatEnableNotify =
            android.bluetooth.BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
    }
}
