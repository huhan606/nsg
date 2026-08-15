# NSG - ESP32 impl

## Hardware

Supported boards:
- **ESP32 WROOM 32E** with u-blox GNSS module — fully functional (serial-based pairing flow; no display)

> **M5Stack Core2 support has been removed.** The M5Stack Core2 board implementation
> (`src/boards/m5stack-core2/`, its environments in `platformio.ini`, and the 16 MB
> partition table) was removed to keep the project focused on custom hardware. If you
> need the Core2 code, it is preserved in the `core2-backup` tag:
> ```bash
> git fetch origin tag core2-backup
> git checkout core2-backup -- esp32/
> ```

Supported GNSS:
- u-blox NEO-M10 GPS module (or anything speaks the same protocol)

## Software approach

- Use PlatformIO with the Arduino framework.
- A board abstraction layer (`src/boards/Board.h`) decouples application logic from board-specific hardware. Each board provides its own implementation in `src/boards/<board>/Board.cpp`, and only the selected board's sources are compiled via `build_src_filter` in `platformio.ini`.
- The core-1 `loop()` only processes GNSS UART + RTC time-sync and draws the screen. All application-level BLE work (scan-queue handling, (re)connect/handshake, TIME/GEO broadcast) runs in a dedicated FreeRTOS task pinned to core 0 (`BleWorker`), so a (re)connect — which can block for up to 45s — never freezes the UI.
- Drain the GPS serial buffer at the start of every loop so no NMEA data is missed.

### Build environments

No `default_envs` is set — specify the environment explicitly:

| Environment | Board |
|---|---|
| `esp32-wroom-32e-release` / `esp32-wroom-32e-debug` | ESP32 WROOM 32E |
| `native` | Host machine (unit tests) |

Example: `pio run -e esp32-wroom-32e-release`

Setup:
+ Detect boot mode (short pin 19 to GND for pairing mode)
+ Call setup_pair() or setup_normal()

Setup Pair:
+ Initialize BLE scan
+ Pairing, saving
+ Exit (reboot)

Setup Normal:
+ Start BLE scan, set handler to filter device and check device in manufacture data, put device address to somewhere
+ Setup GPS, set GPS fix = false

Loop (core 1):
+ Process GPS output, save GPS location and fix, update RTC
+ Draw status screen
+ Push GNSS/RTC snapshot to the BLE worker

BLE worker task (core 0):
+ Scan for pending device, do handshake for each one of them
+ For each connected device, check timer and send GPS payload (every 30s)
+ If got 0x80 or other error from camera, disconnect

Structure:
+ Board abstraction (`src/boards/Board.h`) — board-agnostic interface for setup, loop, pairing UI, normal mode status, and RTC access
+ Board implementations (`src/boards/esp32-wroom-32e/`)
+ SavedCamera (camera name, device, nonce)
+ PendingCamera (camera name, address)
+ ConnectedCamera (camera name, last gps push)

## Modes

The device has two modes:

1. **Pairing mode** — triggered during the boot splash by shorting pin 19 to GND during the 2-second detection window. Scans for a new Nikon camera, runs the 4-stage BLE handshake, bonds over Bluetooth Classic, and saves the camera info.
2. **Normal mode** — the default. Scans for saved cameras, reconnects when in range, and sends the 41-byte GPS payload to the camera whenever a fresh GPS fix is available. Should support multiple cameras connecting at the same time.

## RTC

The ESP32 WROOM 32E has no battery-backed external RTC, so it uses the chip's internal RTC with `TZ=GMT`. On cold boot the system clock reads 1970-01-01 until GNSS syncs it via `setRTC`. The `esp32-wroom-32e-*` environments select the internal 8.5 MHz oscillator divided by 256 (`CONFIG_RTC_CLK_SRC_INT_8MD256=y`) for better timekeeping accuracy at the cost of slightly higher deep-sleep current.
