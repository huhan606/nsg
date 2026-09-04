package info.skyblond.nsp.service

import android.content.Context
import android.content.Intent
import android.location.Location
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.Collections
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class TrackPoint(
    val latitude: Double,
    val longitude: Double,
    val altitude: Double,
    val timeEpochMs: Long
)

/**
 * Lightweight GPX 1.1 track recorder for photography field logs.
 * Includes deadband and accuracy filtering to prevent stationary GPS jitter/drift.
 */
object GpxTrackLogger {

    /** Minimum displacement in meters required to record a new moving point (avoids stationary GNSS drift). */
    const val MIN_MOVE_DISTANCE_METERS = 4.5

    /** Discard fixes with poor accuracy (>25m) to avoid erratic spikes. */
    const val MAX_ALLOWABLE_ACCURACY_METERS = 25.0f

    /** Max time (5 minutes) before a stationary point is recorded as a dwell milestone. */
    const val STATIONARY_DWELL_INTERVAL_MS = 5 * 60 * 1000L

    private val points = Collections.synchronizedList(mutableListOf<TrackPoint>())

    fun addPoint(latitude: Double, longitude: Double, altitude: Double, timeEpochMs: Long = System.currentTimeMillis()): Boolean {
        synchronized(points) {
            val last = points.lastOrNull()
            if (last != null) {
                val dist = haversineMeters(last.latitude, last.longitude, latitude, longitude)
                val timeDelta = timeEpochMs - last.timeEpochMs
                // Filter out small jitter if within stationary deadband and not past dwell interval
                if (dist < MIN_MOVE_DISTANCE_METERS && timeDelta < STATIONARY_DWELL_INTERVAL_MS) {
                    return false
                }
            }
            points.add(TrackPoint(latitude, longitude, altitude, timeEpochMs))
            return true
        }
    }

    fun addPoint(location: Location): Boolean {
        if (location.hasAccuracy() && location.accuracy > MAX_ALLOWABLE_ACCURACY_METERS) {
            return false
        }
        val timeMs = if (location.time > 0) location.time else System.currentTimeMillis()
        return addPoint(
            latitude = location.latitude,
            longitude = location.longitude,
            altitude = location.altitude,
            timeEpochMs = timeMs
        )
    }

    fun clear() {
        points.clear()
    }

    fun pointCount(): Int = points.size

    fun getPoints(): List<TrackPoint> = synchronized(points) { points.toList() }

    data class TrackSummary(
        val pointCount: Int,
        val totalDistanceMeters: Double,
        val minAltitude: Double,
        val maxAltitude: Double,
        val startTimeEpochMs: Long,
        val endTimeEpochMs: Long
    )

    fun getTrackSummary(): TrackSummary? {
        val pts = getPoints()
        if (pts.isEmpty()) return null
        var totalDist = 0.0
        var minEle = pts.first().altitude
        var maxEle = pts.first().altitude
        for (i in 0 until pts.size - 1) {
            val p1 = pts[i]
            val p2 = pts[i + 1]
            totalDist += haversineMeters(p1.latitude, p1.longitude, p2.latitude, p2.longitude)
            if (p2.altitude < minEle) minEle = p2.altitude
            if (p2.altitude > maxEle) maxEle = p2.altitude
        }
        return TrackSummary(
            pointCount = pts.size,
            totalDistanceMeters = totalDist,
            minAltitude = minEle,
            maxAltitude = maxEle,
            startTimeEpochMs = pts.first().timeEpochMs,
            endTimeEpochMs = pts.last().timeEpochMs
        )
    }

    /**
     * Calculates great-circle distance between two coordinates in meters.
     */
    fun haversineMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371000.0 // Earth radius in meters
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
            sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }

    fun buildGpxXml(): String? {
        val currentPoints = synchronized(points) { points.toList() }
        if (currentPoints.isEmpty()) return null

        val sdfIso = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }

        val sb = StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8"?>""").append("\n")
        sb.append("""<gpx version="1.1" creator="Nikon Smart GPS" xmlns="http://www.topografix.com/GPX/1/1">""").append("\n")
        sb.append("  <trk>\n")
        sb.append("    <name>Nikon Smart GPS Track</name>\n")
        sb.append("    <trkseg>\n")

        for (pt in currentPoints) {
            val timeStr = sdfIso.format(Date(pt.timeEpochMs))
            sb.append(String.format(Locale.US, "      <trkpt lat=\"%.6f\" lon=\"%.6f\">\n", pt.latitude, pt.longitude))
            sb.append(String.format(Locale.US, "        <ele>%.1f</ele>\n", pt.altitude))
            sb.append("        <time>$timeStr</time>\n")
            sb.append("      </trkpt>\n")
        }

        sb.append("    </trkseg>\n")
        sb.append("  </trk>\n")
        sb.append("</gpx>\n")
        return sb.toString()
    }

    fun exportGpx(context: Context): File? {
        val xml = buildGpxXml() ?: return null
        val dir = File(context.cacheDir, "tracks").apply { mkdirs() }
        val sdfFile = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
        val file = File(dir, "nikon_track_${sdfFile.format(Date())}.gpx")
        file.writeText(xml, Charsets.UTF_8)
        return file
    }

    fun shareGpx(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/gpx+xml"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, file.name)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val chooser = Intent.createChooser(intent, "导出 GPX 轨迹").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }
}
