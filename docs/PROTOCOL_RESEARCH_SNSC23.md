# SNSC 2.3 / G30 protocol research and implementation status

Updated: 2026-10-30

## Critical finding: SHFW itself is not open source

Public SHFW documentation describes features and configuration through ScooterHacking Utility, but the SHFW firmware/API implementation is closed-source. A public, verified SHFW live-tuning command map for phase current, battery current, field weakening, brake/throttle boost, profile switching, or the SNSC 2.3 deck latch was not found. We must not present guessed register IDs as a working SHFW implementation.

References:
- Official ScooterHacking site / SHFW overview: https://cfw.sh/
- Utility: https://utility.cfw.sh/
- SHFW overview and source status: https://joeybabcock.me/wiki/SHFW
- Public G30 protocol reconstruction: https://github.com/Vicy-DE/Ninebot-G30-Custom/blob/main/docs/protocol.md
- Public G30 register map: https://github.com/Vicy-DE/Ninebot-G30-Custom/blob/main/docs/REGISTER_MAP.md
- Community tuning map (confidence varies by register/model): https://github.com/jx-grxf/scooter-tuning-db

## What was wrong in the previous app implementation

The previous `NinebotProtocol.kt` used invented register IDs `0xB0–0xD3` for lock, telemetry, tuning, and diagnostics; it also treated the payload length and command values incorrectly for the documented G30 framing. Its parser checked only the header and did not verify length/checksum. The BLE manager updated local UI state immediately even if the controller had not acknowledged a write.

## Changes in this branch

- Implemented the documented `5A A5` frame layout: LEN counts payload bytes only, source address `0x3E` (app), destination `0x20` (ESC), and checksum `sum(LEN..payload) XOR 0xFFFF`, little-endian.
- Uses documented G30 read command `0x01` and write-with-response command `0x02`.
- The speed-mode setter uses the documented G30 riding-mode register `0x75` with values Eco=0, Drive=1, Sport=2.
- Adds response parsing with frame-length and checksum validation.
- Disables all guessed SHFW tuning writes and the guessed movement-lock operation. The deck/battery latch is not the same thing as the scooter movement lock.
- Does not claim that a valid generic G30 frame proves SNSC 2.3 compatibility.

## Remaining blockers before calling this "working"

1. **BLE transport/GATT UUIDs:** the app currently uses Nordic UART Service UUIDs. These must be verified against the actual Bluetooth service/characteristics advertised by the user's controller/dashboard. G30 UART framing does not prove the BLE GATT service is Nordic UART.
2. **SNSC 2.3 firmware compatibility:** standard G30 register semantics must be verified against the exact controller firmware that is currently flashed.
3. **SHFW live tuning:** requires documented commands or a captured request/response session from a compatible SHU/Luna + controller combination. Standard G30 registers are not a substitute for SHFW's private configuration interface.
4. **Acknowledgement/readback:** state should be updated only after the correct response command/register/value is received. Current UI/manager still needs a request queue and readback-based state reconciliation.
5. **Deck lock:** no verified SNSC 2.3 battery/deck-latch command has been identified. Do not send the `0x81` “Eject Battery” lead from unrelated `A3 A4` protocol captures through the `5A A5` builder.

## Safety and validation

Tuning writes remain disabled until their actual protocol is verified. Do not test guessed current or field-weakening commands on a powered scooter. Validate packet framing with captured read-only traffic first, then confirm register reads, acknowledgements, and readback on the exact controller/firmware.
