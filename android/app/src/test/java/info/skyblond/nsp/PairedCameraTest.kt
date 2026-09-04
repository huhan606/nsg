package info.skyblond.nsp

import info.skyblond.nsp.data.PairedCamera
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PairedCameraTest {

    @Test
    fun displayNameFallsBackToHardwareNameWhenNoCustomAlias() {
        val cam = PairedCamera(
            name = "Z 8_1234567",
            address = "AA:BB:CC:DD:EE:FF",
            addressType = 1,
            device = 12345L,
            nonce = 67890L,
            controllerName = "Pixel 8"
        )
        assertNull(cam.customName)
        assertEquals("Z 8_1234567", cam.displayName)
    }

    @Test
    fun displayNameUsesCustomAliasWhenPresent() {
        val cam = PairedCamera(
            name = "Z 8_1234567",
            address = "AA:BB:CC:DD:EE:FF",
            addressType = 1,
            device = 12345L,
            nonce = 67890L,
            controllerName = "Pixel 8",
            customName = "My Studio Z8"
        )
        assertEquals("My Studio Z8", cam.customName)
        assertEquals("My Studio Z8", cam.displayName)
    }

    @Test
    fun displayNameFallsBackToHardwareNameWhenCustomAliasIsBlank() {
        val cam = PairedCamera(
            name = "Z 8_1234567",
            address = "AA:BB:CC:DD:EE:FF",
            addressType = 1,
            device = 12345L,
            nonce = 67890L,
            controllerName = "Pixel 8",
            customName = "   "
        )
        assertEquals("Z 8_1234567", cam.displayName)
    }


    @Test
    fun preserveExistingCustomNameWhenIncomingIsNull() {
        val existing = PairedCamera(
            name = "NIKON Z 7_2",
            address = "11:22:33:44:55:66",
            addressType = 2,
            device = 0x12345678L,
            nonce = 0x87654321L,
            controllerName = "Android_Test",
            customName = "Studio Camera"
        )
        // Simulated onBonded or reconnect incoming object without customName
        val incoming = PairedCamera(
            name = "NIKON Z 7_2",
            address = "66:55:44:33:22:11", // updated BLE address
            addressType = 2,
            device = 0x12345678L,
            nonce = 0x99999999L,
            controllerName = "Android_Test"
        )
        assertNull(incoming.customName)

        val merged = if (incoming.customName == null) {
            incoming.copy(customName = existing.customName)
        } else {
            incoming
        }

        assertEquals("Studio Camera", merged.customName)
        assertEquals("Studio Camera", merged.displayName)
        assertEquals("66:55:44:33:22:11", merged.address)
    }
}
