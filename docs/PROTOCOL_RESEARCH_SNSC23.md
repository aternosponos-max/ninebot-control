# SNSC 2.3 / G30 BLE protocol research

## Findings (2026-10-30)

The current `NinebotProtocol.kt` implementation is not a working SHFW protocol. It invents register IDs for movement lock, tuning, diagnostics and telemetry and assumes a frame format without performing authentication or validating responses. The UI currently changes local state immediately after writing, which can falsely imply that the scooter accepted a command.

### Public references

- `scooter-tuning-db/ninebot.json`: community map lists G30-family registers `0x74` (riding mode), `0x93` (speed limit), `0xB5` (battery percentage), `0xB7` (odometer), and `0xB9` (trip mileage). It warns to read before writing and notes that firmware/version coverage varies.
- `sinimiini/scooter-tuuner`: documents a `5A A5` register frame, checksum `0xFFFF XOR sum(inner)`, and example registers for some G30-like controllers. This is a separate community implementation, not proof that the SNSC 2.3 + flashed G30 firmware supports every register.
- `MR-Berglund/loll`: describes a different `A3 A4` command family with per-frame random masking and CRC8. It lists `0x81` as “Eject Battery”, but the repository is a capture-tool skeleton and does not establish that this command actuates the physical deck lock on SNSC 2.3.

## Important protocol separation

SHFW is firmware plus its supported settings, not a universal BLE register list. The app must not label invented `0xC0–0xC9` registers as SHFW settings. A register map for a G30-family controller is not automatically a map for the deck-lock actuator.

## Deck lock

No verified, model-specific SNSC 2.3 deck-lock command has been found in the reviewed public sources. `0x81` / “Eject Battery” is only a lead for further verification, not a safe command to send: the required `A3 A4` authentication/masking and CRC8 path is different from the current `5A A5` builder.

Do not add a guessed write to the battery/deck lock. Confirm it against a capture from a compatible app/controller or a protocol implementation tested on the same hardware/firmware first.

## Required engineering changes

1. Implement the actual authentication/session flow for the detected BLE generation.
2. Use a model/firmware-specific register map.
3. Validate response framing/checksum and parse returned register values.
4. Update UI state only after a valid ACK/readback; otherwise show a command failure.
5. Keep unverified tuning and deck-lock operations disabled rather than sending placeholder packets.

## Sources

- https://github.com/jx-grxf/scooter-tuning-db
- https://github.com/sinimiini/scooter-tuuner
- https://github.com/MR-Berglund/loll
- https://github.com/scooterhacking/wiki/blob/main/pages/shfw020guide.txt
