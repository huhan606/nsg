#include "StatusLED.h"

#include <Arduino.h>

StatusLED statusLed;

void StatusLED::begin() {
    // R, G, B on three LEDC channels, 5000 Hz, 8-bit resolution
    ledcAttach(STATUS_LED_PIN_R, 5000, 8);
    ledcAttach(STATUS_LED_PIN_G, 5000, 8);
    ledcAttach(STATUS_LED_PIN_B, 5000, 8);
    setColor(STATUS_LED_OFF, STATUS_LED_OFF, STATUS_LED_OFF);
}

void StatusLED::setColor(uint8_t r, uint8_t g, uint8_t b) {
    ledcWrite(STATUS_LED_PIN_R, r);
    ledcWrite(STATUS_LED_PIN_G, g);
    ledcWrite(STATUS_LED_PIN_B, b);
}
