#include "LongPressButton.h"

#include <Arduino.h>

LongPressButton::LongPressButton(uint8_t pin) : pin(pin) {}

void LongPressButton::begin() { pinMode(pin, INPUT_PULLUP); }

void LongPressButton::reset() {
    const bool rawPressed = digitalRead(pin) == LOW;
    lastRaw = rawPressed;
    debouncedPressed = rawPressed;
    lastRawChangeAt = millis();
    pressStartAt = millis();
    // a button held across boot must not arm the long-press timer
    triggered = rawPressed;
}

bool LongPressButton::update() {
    const bool rawPressed = digitalRead(pin) == LOW;
    const uint32_t now = millis();

    if (rawPressed != lastRaw) {
        lastRaw = rawPressed;
        lastRawChangeAt = now;
    }

    // input is stable long enough: accept the new state
    if ((uint32_t)(now - lastRawChangeAt) >= BUTTON_DEBOUNCE_MS && debouncedPressed != lastRaw) {
        debouncedPressed = lastRaw;
        if (debouncedPressed) {
            // debounced press: start long-press timing
            pressStartAt = now;
            triggered = false;
        }
    }

    if (debouncedPressed && !triggered && (uint32_t)(now - pressStartAt) >= BUTTON_LONG_PRESS_MS) {
        triggered = true;
        return true;
    }
    return false;
}
