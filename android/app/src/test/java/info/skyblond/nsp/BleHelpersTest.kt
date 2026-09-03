package info.skyblond.nsp

import info.skyblond.nsp.service.BleHelpers
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Test

class BleHelpersTest {

    @Test
    fun generatedSnapBridgeControllerNameDoesNotExceed31Chars() {
        repeat(50) {
            val name = BleHelpers.generateSnapBridgeControllerName()
            assertTrue("Name length (${name.length}) should be <= 31: $name", name.length <= 31)
            assertTrue("Name should start with Android_: $name", name.startsWith("Android_"))
        }
    }

    @Test
    fun namesMatchHandlesPrefixAndExactCase() {
        assertTrue(BleHelpers.namesMatch("Z50_2_1234", "z50_2_1234"))
        assertTrue(BleHelpers.namesMatch("Z50_2_1234", "Z50_2"))
        assertTrue(BleHelpers.namesMatch("Z50_2", "Z50_2_1234"))
        assertEquals(false, BleHelpers.namesMatch("Z50", "D850"))
    }
}
