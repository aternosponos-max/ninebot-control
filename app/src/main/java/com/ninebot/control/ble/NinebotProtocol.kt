package com.ninebot.control.ble

/**
 * Open Ninebot UART packet framing for the G30 family.
 *
 * This is NOT an implementation of the closed-source SHFW tuning API. Public
 * sources document standard G30 registers, but do not document SHFW live-tuning
 * commands. Unverified tuning writes are therefore deliberately disabled.
 *
 * Frame: 5A A5 | payloadLength | src | dst | cmd | arg | payload | checksumLE
 * LEN counts payload only; total frame size = LEN + 9.
 * Checksum = (sum(bytes from LEN through payload) XOR FFFF), little-endian.
 *
 * References:
 * https://github.com/Vicy-DE/Ninebot-G30-Custom/blob/main/docs/protocol.md
 * https://github.com/Vicy-DE/Ninebot-G30-Custom/blob/main/docs/REGISTER_MAP.md
 */
object NinebotProtocol {
    private const val HEADER_1 = 0x5A
    private const val HEADER_2 = 0xA5
    private const val SRC_APP = 0x3E
    private const val DST_ESC = 0x20

    private const val CMD_READ = 0x01
    private const val CMD_WRITE_ACK = 0x02

    const val supportsShfwTuning: Boolean = false

    object Registers {
        const val BATTERY_LEVEL = 0x22
        const val CURRENT_SPEED = 0x26
        const val TOTAL_DISTANCE = 0x29
        const val BATTERY_VOLTAGE = 0x48
        const val BATTERY_CURRENT = 0x49
        const val RIDING_MODE = 0x75
        const val NORMAL_SPEED_LIMIT = 0x73
        const val ECO_SPEED_LIMIT = 0x74
        const val KERS_LEVEL = 0x7B
        const val CRUISE_CONTROL = 0x7C
    }

    data class RegisterResponse(
        val command: Int,
        val register: Int,
        val payload: ByteArray,
        val source: Int,
        val destination: Int
    )

    fun buildSetSpeedModeFrame(mode: SpeedMode): ByteArray {
        val value = when (mode) {
            SpeedMode.MODE_1 -> 0x00 // Eco
            SpeedMode.MODE_2 -> 0x01 // Drive/Normal
            SpeedMode.MODE_3 -> 0x02 // Sport
        }
        return buildWriteRegisterFrame(Registers.RIDING_MODE, byteArrayOf(value.toByte()))
    }

    // This is a scooter movement lock, not the physical battery/deck latch.
    // Exact behavior on SNSC 2.3 is unverified, so no command is emitted.
    fun buildSetMovementLockFrame(locked: Boolean): ByteArray = ByteArray(0)

    /** Read known battery-level register 0x22 as a basic protocol check. */
    fun buildReadTelemetryFrame(): ByteArray = buildReadRegisterFrame(Registers.BATTERY_LEVEL, 2)

    fun buildReadRegisterFrame(register: Int, length: Int): ByteArray {
        require(register in 0..0xFF)
        require(length in 1..0xFF)
        return buildFrame(CMD_READ, register, byteArrayOf(length.toByte(), 0x00))
    }

    fun buildWriteRegisterFrame(register: Int, payload: ByteArray): ByteArray {
        require(register in 0..0xFF)
        require(payload.isNotEmpty() && payload.size <= 0xFF)
        return buildFrame(CMD_WRITE_ACK, register, payload)
    }

    // SHFW-specific tuning IDs are not publicly verified. Never send guessed 0xC0–0xC9 writes.
    @Deprecated("SHFW protocol is not public/verified; write disabled")
    fun buildSetPhaseCurrentFrame(amps: Int): ByteArray = ByteArray(0)
    @Deprecated("SHFW protocol is not public/verified; write disabled")
    fun buildSetBatteryCurrentFrame(amps: Int): ByteArray = ByteArray(0)
    @Deprecated("SHFW protocol is not public/verified; write disabled")
    fun buildSetSpeedLimitOverrideFrame(kmh: Int): ByteArray = ByteArray(0)
    @Deprecated("SHFW protocol is not public/verified; write disabled")
    fun buildSetFieldWeakeningFrame(enabled: Boolean): ByteArray = ByteArray(0)
    @Deprecated("SHFW protocol is not public/verified; write disabled")
    fun buildSetCruiseControlFrame(enabled: Boolean): ByteArray = ByteArray(0)
    @Deprecated("SHFW protocol is not public/verified; write disabled")
    fun buildSetRegenCurrentFrame(amps: Int): ByteArray = ByteArray(0)
    @Deprecated("SHFW protocol is not public/verified; write disabled")
    fun buildSetBrakeBoostFrame(percent: Int): ByteArray = ByteArray(0)
    @Deprecated("SHFW protocol is not public/verified; write disabled")
    fun buildSetThrottleBoostFrame(percent: Int): ByteArray = ByteArray(0)
    @Deprecated("SHFW protocol is not public/verified; write disabled")
    fun buildSetAccelBoostFrame(percent: Int): ByteArray = ByteArray(0)
    @Deprecated("SHFW profile protocol is not public/verified; write disabled")
    fun buildSelectProfileFrame(profile: TuningProfile): ByteArray = ByteArray(0)

    fun buildReadCellVoltagesFrame(): ByteArray = ByteArray(0)
    fun buildReadErrorCodesFrame(): ByteArray = buildReadRegisterFrame(0x1B, 2)
    fun buildReadFirmwareInfoFrame(): ByteArray = buildReadRegisterFrame(0x1A, 2)
    fun buildReadRideStatsFrame(): ByteArray = buildReadRegisterFrame(Registers.TOTAL_DISTANCE, 4)

    private fun buildFrame(cmd: Int, register: Int, payload: ByteArray): ByteArray {
        val body = byteArrayOf(
            payload.size.toByte(), SRC_APP.toByte(), DST_ESC.toByte(),
            cmd.toByte(), register.toByte()
        ) + payload
        val checksum = computeChecksum(body)
        return byteArrayOf(HEADER_1.toByte(), HEADER_2.toByte()) + body + checksum
    }

    private fun computeChecksum(data: ByteArray): ByteArray {
        var sum = 0
        data.forEach { sum = (sum + (it.toInt() and 0xFF)) and 0xFFFF }
        val checksum = sum xor 0xFFFF
        return byteArrayOf((checksum and 0xFF).toByte(), ((checksum ushr 8) and 0xFF).toByte())
    }

    /** Parse a complete frame and validate its length and checksum. */
    fun tryParseTelemetry(bytes: ByteArray): PartialTelemetry? {
        if (bytes.size < 9) return null
        if ((bytes[0].toInt() and 0xFF) != HEADER_1 ||
            (bytes[1].toInt() and 0xFF) != HEADER_2) return null
        val payloadLength = bytes[2].toInt() and 0xFF
        if (bytes.size != payloadLength + 9) return null

        val checksum = computeChecksum(bytes.copyOfRange(2, bytes.size - 2))
        if (bytes[bytes.size - 2] != checksum[0] ||
            bytes[bytes.size - 1] != checksum[1]) return null

        val response = RegisterResponse(
            command = bytes[5].toInt() and 0xFF,
            register = bytes[6].toInt() and 0xFF,
            payload = bytes.copyOfRange(7, bytes.size - 2),
            source = bytes[3].toInt() and 0xFF,
            destination = bytes[4].toInt() and 0xFF
        )
        return PartialTelemetry(bytes.copyOf(), response)
    }

    fun toHex(bytes: ByteArray): String =
        bytes.joinToString(" ") { "%02X".format(it.toInt() and 0xFF) }

    data class PartialTelemetry(val raw: ByteArray, val response: RegisterResponse)
}
