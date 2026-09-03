package info.skyblond.nsp

import info.skyblond.nsp.service.GpxTrackLogger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GpxTrackLoggerTest {

    @Before
    fun setUp() {
        GpxTrackLogger.clear()
    }

    @Test
    fun addPointIncrementsCountAndClears() {
        assertEquals(0, GpxTrackLogger.pointCount())
        assertNull(GpxTrackLogger.buildGpxXml())

        GpxTrackLogger.addPoint(35.6895, 139.6917, 45.0, 1700000000000L)
        assertEquals(1, GpxTrackLogger.pointCount())

        val xml = GpxTrackLogger.buildGpxXml()
        assertNotNull(xml)
        assertTrue(xml!!.contains("<gpx version=\"1.1\""))
        assertTrue(xml.contains("lat=\"35.689500\""))
        assertTrue(xml.contains("lon=\"139.691700\""))
        assertTrue(xml.contains("<ele>45.0</ele>"))

        GpxTrackLogger.clear()
        assertEquals(0, GpxTrackLogger.pointCount())
        assertNull(GpxTrackLogger.buildGpxXml())
    }
}
