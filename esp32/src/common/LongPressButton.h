#ifndef LONG_PRESS_BUTTON_H
#define LONG_PRESS_BUTTON_H

#include <stdint.h>

#ifndef BUTTON_DEBOUNCE_MS
#define BUTTON_DEBOUNCE_MS 50
#endif

#ifndef BUTTON_LONG_PRESS_MS
#define BUTTON_LONG_PRESS_MS 3000
#endif

/**
 * Debounced active-low long-press detector for a button wired between the pin
 * and GND (INPUT_PULLUP). Call update() every loop iteration; it returns true
 * exactly once when the button has been held for BUTTON_LONG_PRESS_MS.
 */
class LongPressButton {
   public:
    explicit LongPressButton(uint8_t pin);
    void begin();
    /**
     * Re-synchronize the internal state with the current pin level, so that a
     * button still held from before (e.g. held across a reboot) does not count
     * as a fresh press. Call at the end of setup().
     */
    void reset();
    bool update();

   private:
    const uint8_t pin;
    bool lastRaw = false;
    bool debouncedPressed = false;
    bool triggered = false;
    uint32_t lastRawChangeAt = 0;
    uint32_t pressStartAt = 0;
};

#endif  // LONG_PRESS_BUTTON_H
