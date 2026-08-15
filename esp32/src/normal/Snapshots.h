#ifndef SNAPSHOTS_H
#define SNAPSHOTS_H

#include <cstdint>

// GNSS
struct GnssSnapshot {
    double lat = 0;
    double lon = 0;
    int32_t altitudeMeters = 0;
    uint8_t satellites = 0;
    uint8_t gnssValid = 0;
};

// RTC
struct RtcSnapshot {
    uint16_t year = 0;
    uint8_t month = 0;
    uint8_t day = 0;
    uint8_t hour = 0;
    uint8_t minute = 0;
    uint8_t second = 0;
};

// BLE status
struct BleStatusSnapshot {
    uint32_t activeConnections = 0;
    uint32_t pairedCount = 0;
};

#endif  // SNAPSHOTS_H
