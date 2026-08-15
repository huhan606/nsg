#include <Arduino.h>
#include <BLEDevice.h>

#include <cstdlib>

#include "BootMode.h"
#include "Config.h"
#include "Esp32RandomGenerator.h"
#include "Logging.h"
#include "Utils.h"
#include "common/LongPressButton.h"
#include "normal/NormalMode.h"
#include "pairing/PairingMode.h"

// boot mode enum
enum class BootModeEnum { PAIRING, NORMAL };

#ifndef BOOTMODE_DETECT_PIN
#define BOOTMODE_DETECT_PIN 19
#endif

#ifndef BOOTMODE_DETECT_DELAY_MS
#define BOOTMODE_DETECT_DELAY_MS 2000
#endif

BootModeEnum bootModeType = BootModeEnum::NORMAL;
NormalMode* normalMode = nullptr;
PairingMode* pairingMode = nullptr;
LongPressButton bootModeButton(BOOTMODE_DETECT_PIN);

void setup() {
    // enable default serial as monitor
    Serial.begin(115200);
    NSG_LOG_DEBUG("MainSetup", "Serial initialized");

    // init ESP32 internal RTC, use GMT timezone
    // (the system clock is backed by the internal RTC; no battery, so it
    //  reads 1970-01-01 on cold boot until GNSS syncs it via setRTC)
    setenv("TZ", "GMT", 1);
    tzset();
    NSG_LOG_DEBUG("MainSetup", "Internal RTC initialized (TZ=GMT)");
    // setup pin for boot mode detection
    bootModeButton.begin();

    // init BLE stack (required by both boot modes)
    Esp32RandomGenerator rnd;
    const uint32_t id = Config::getOrGenerateId(rnd);
    const std::string bleDeviceName = Utils::generateFullId(id);
    NSG_LOG_INFO("MainSetup", "BLE device name: %s", bleDeviceName.c_str());
    if (!BLEDevice::init(String(bleDeviceName.c_str()))) {
        NSG_LOG_FATAL("MainSetup", "failed to initialize BLE");
    }

    // collect boot up mode
    if (Config::hasPairingFlag()) {
        // long-press in normal mode requested pairing; clear the flag so the
        // reboot after pairing (or any later reboot) goes back to normal
        Config::clearPairingFlag();
        NSG_LOG_INFO("MainSetup", "Pairing flag set, entering Pairing mode");
        bootModeType = BootModeEnum::PAIRING;
    } else {
        NSG_LOG_INFO("MainSetup", "Detecting boot mode... Short pin %d to GND to enter pairing mode", BOOTMODE_DETECT_PIN);
        // wait for a while and read detect pin
        delay(BOOTMODE_DETECT_DELAY_MS);

        // if short to GND (read 0) -> pairing mode
        if (!digitalRead(BOOTMODE_DETECT_PIN)) {
            NSG_LOG_INFO("MainSetup", "Entering Pairing mode");
            bootModeType = BootModeEnum::PAIRING;
        } else {
            NSG_LOG_INFO("MainSetup", "Entering Normal mode");
            bootModeType = BootModeEnum::NORMAL;
        }
    }

    switch (bootModeType) {
        case BootModeEnum::NORMAL:
            normalMode = new NormalMode();
            normalMode->setup();
            break;

        case BootModeEnum::PAIRING:
            pairingMode = new PairingMode();
            pairingMode->setup();
            break;

        default:
            NSG_LOG_FATAL("MainSetup", "Unexpected boot type");
            break;
    }

    // discard any button state from before the reset (e.g. the boot-detect
    // hold), so a button held through reboot doesn't long-press again
    bootModeButton.reset();
}

void loop() {
    // long-press handling: in normal mode, request pairing mode on the next
    // boot; in pairing mode, just reboot back to normal mode
    if (bootModeButton.update()) {
        if (bootModeType == BootModeEnum::NORMAL) {
            NSG_LOG_INFO("MainLoop", "Long press detected, rebooting into pairing mode...");
            Config::setPairingFlag();
        } else {
            NSG_LOG_INFO("MainLoop", "Long press detected, rebooting into normal mode...");
        }
        ESP.restart();
    }

    switch (bootModeType) {
        case BootModeEnum::NORMAL:
            if (normalMode) {
                normalMode->loop();
            } else {
                NSG_LOG_FATAL("MainLoop", "Boot type normal but nullptr");
            }
            break;

        case BootModeEnum::PAIRING:
            if (pairingMode) {
                pairingMode->loop();
            } else {
                NSG_LOG_FATAL("MainLoop", "Boot type pairing but nullptr");
            }
            break;

        default:
            NSG_LOG_FATAL("MainLoop", "Unexpected boot type");
            break;
    }
}
