#ifndef CONFIG_H
#define CONFIG_H

#include <ArduinoJson.h>
#include <stdint.h>

#include <string>
#include <vector>

class SavedCameraInfo {
   public:
    SavedCameraInfo(String bleName, uint32_t device, uint32_t nonce);
    String bleName;
    uint32_t device;
    uint32_t nonce;
    void addToJsonArray(JsonDocument& parent) const;
};

class RandomGenerator;

namespace Config {

/**
 * Get the persisted device ID, generating and saving one if it does not exist.
 */
uint32_t getOrGenerateId(RandomGenerator& randomGenerator);

std::vector<SavedCameraInfo> getSavedCameras();

void addToSavedCameras(const SavedCameraInfo& cameraInfo);

/**
 * Flag written by the normal-mode long-press to request pairing mode on the
 * next boot. Read once at boot and cleared before entering pairing mode.
 */
bool hasPairingFlag();
void setPairingFlag();
void clearPairingFlag();

}  // namespace Config

#endif  // CONFIG_H
