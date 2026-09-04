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

    @Test
    fun stationaryJitterIsFilteredOut() {
        // First fix at (35.689500, 139.691700)
        val added1 = GpxTrackLogger.addPoint(35.689500, 139.691700, 45.0, 10000L)
        assertTrue(added1)
        assertEquals(1, GpxTrackLogger.pointCount())

        // Micro-jitter: ~1 meter away 2 seconds later -> should be filtered out
        val added2 = GpxTrackLogger.addPoint(35.689508, 139.691700, 45.0, 12000L)
        org.junit.Assert.assertFalse(added2)
        assertEquals(1, GpxTrackLogger.pointCount())

        // Significant movement: ~50 meters away -> should be recorded
        val added3 = GpxTrackLogger.addPoint(35.690000, 139.691700, 45.0, 15000L)
        assertTrue(added3)
        assertEquals(2, GpxTrackLogger.pointCount())
    }
}
