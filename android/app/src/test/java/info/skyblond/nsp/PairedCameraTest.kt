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
}
