package info.skyblond.nsp.ble

import org.junit.Assert.assertEquals
import org.junit.Test

class GeoPayloadGeneratorTest {

    @Test
    fun generatedPayloadIs41Bytes() {
        val payload = GeoPayloadGenerator.buildFake()
        assertEquals(41, payload.size)
    }

    @Test
    fun headerIsCorrect() {
        val payload = GeoPayloadGenerator.buildFake()
        // 0x007F little-endian: first byte 0x7F, second byte 0x00
        assertEquals(0x7F.toByte(), payload[0])
        assertEquals(0x00.toByte(), payload[1])
    }

    @Test
    fun standardFieldIsWgs84() {
        val payload = GeoPayloadGenerator.buildFake()
        val standard = payload.copyOfRange(25, 31).toString(Charsets.US_ASCII)
        assertEquals("WGS-84", standard)
    }

    @Test
    fun positiveAltitudeIsEncodedWithPAndAbsoluteValue() {
        val now = java.time.ZonedDateTime.now(java.time.ZoneOffset.UTC)
        val payload = GeoPayloadGenerator.build(31.23, 121.47, 150.0, now)
        assertEquals('P'.code.toByte(), payload[13])
        val alt = (payload[14].toInt() and 0xFF) or ((payload[15].toInt() and 0xFF) shl 8)
        assertEquals(150, alt)
    }

    @Test
    fun negativeAltitudeIsEncodedWithMAndAbsoluteValue() {
        val now = java.time.ZonedDateTime.now(java.time.ZoneOffset.UTC)
        val payload = GeoPayloadGenerator.build(31.23, 121.47, -28.0, now)
        assertEquals('M'.code.toByte(), payload[13])
        val alt = (payload[14].toInt() and 0xFF) or ((payload[15].toInt() and 0xFF) shl 8)
        assertEquals(28, alt)
    }

    @Test
    fun satellitesAreProperlyEncodedAtOffset12() {
        val now = java.time.ZonedDateTime.now(java.time.ZoneOffset.UTC)
        val payload = GeoPayloadGenerator.build(31.23, 121.47, 10.0, now, satellites = 18)
        assertEquals(18.toByte(), payload[12])

        // Clamped max
        val payloadMax = GeoPayloadGenerator.build(31.23, 121.47, 10.0, now, satellites = 99)
        assertEquals(32.toByte(), payloadMax[12])
    }
}
