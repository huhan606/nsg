#ifndef STATUS_LED_H
#define STATUS_LED_H

#include <stdint.h>

#ifndef STATUS_LED_PIN_R
#define STATUS_LED_PIN_R 21
#endif

#ifndef STATUS_LED_PIN_G
#define STATUS_LED_PIN_G 22
#endif

#ifndef STATUS_LED_PIN_B
#define STATUS_LED_PIN_B 23
#endif

// Common anode wiring: LOW (0) lights the channel, HIGH (255) turns it off.
#ifndef STATUS_LED_ON
#define STATUS_LED_ON 0
#endif

#ifndef STATUS_LED_OFF
#define STATUS_LED_OFF 255
#endif

/**
 * RGB status LED driven by three LEDC PWM channels. Call begin() once at boot
 * (attaches the pins and turns all channels off), then setColor() anywhere.
 */
class StatusLED {
   public:
    /**
     * Attach the R/G/B pins to LEDC channels and turn all channels off.
     */
    void begin();
    /**
     * Write the given 8-bit values to the R/G/B channels. Use the
     * STATUS_LED_ON / STATUS_LED_OFF macros for plain on/off colors.
     */
    void setColor(uint8_t r, uint8_t g, uint8_t b);
};

// Global status LED; any component may include this header and set the status.
extern StatusLED statusLed;

#endif  // STATUS_LED_H
