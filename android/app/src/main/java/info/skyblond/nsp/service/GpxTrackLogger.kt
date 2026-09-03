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

data class TrackPoint(
    val latitude: Double,
    val longitude: Double,
    val altitude: Double,
    val timeEpochMs: Long
)

/**
 * Lightweight GPX 1.1 track recorder for photography field logs.
 */
object GpxTrackLogger {

    private val points = Collections.synchronizedList(mutableListOf<TrackPoint>())

    fun addPoint(latitude: Double, longitude: Double, altitude: Double, timeEpochMs: Long = System.currentTimeMillis()) {
        points.add(TrackPoint(latitude, longitude, altitude, timeEpochMs))
    }

    fun addPoint(location: Location) {
        addPoint(
            latitude = location.latitude,
            longitude = location.longitude,
            altitude = location.altitude,
            timeEpochMs = if (location.time > 0) location.time else System.currentTimeMillis()
        )
    }

    fun clear() {
        points.clear()
    }

    fun pointCount(): Int = points.size

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
