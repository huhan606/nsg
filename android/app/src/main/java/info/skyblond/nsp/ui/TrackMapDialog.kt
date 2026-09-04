package info.skyblond.nsp.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import info.skyblond.nsp.service.GpxTrackLogger
import info.skyblond.nsp.service.TrackPoint
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min

@Composable
fun TrackMapDialog(
    onExportGpx: () -> Unit,
    onClearTrack: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val points = remember { GpxTrackLogger.getPoints() }
    val summary = remember { GpxTrackLogger.getTrackSummary() }

    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.90f)
                .clip(RoundedCornerShape(24.dp)),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF141619)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = L10n.t("📷 拍摄足迹轨迹预览", "📷 Photo Footprint Route Map"),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = L10n.t("实时离线矢量轨迹 · 双指缩放/单指平移", "Offline Vector Route · Pinch to zoom / drag to pan"),
                            fontSize = 11.sp,
                            color = Color(0xFF8E929B)
                        )
                    }
                    TextButton(onClick = onDismiss) {
                        Text(L10n.t("关闭", "Close"), color = Color(0xFFFFE500))
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Stats Dashboard Bar
                if (summary != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF1F2228), RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        StatItem(
                            label = L10n.t("总里程", "Distance"),
                            value = String.format(java.util.Locale.US, "%.2f km", summary.totalDistanceMeters / 1000.0)
                        )
                        StatItem(
                            label = L10n.t("记录点数", "Points"),
                            value = "${summary.pointCount}"
                        )
                        StatItem(
                            label = L10n.t("海拔区间", "Elevation"),
                            value = String.format(java.util.Locale.US, "%.0f~%.0fm", summary.minAltitude, summary.maxAltitude)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Vector Canvas Map View
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF0D0E11))
                        .border(1.dp, Color(0xFF2B2E36), RoundedCornerShape(16.dp))
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                scale = (scale * zoom).coerceIn(0.5f, 5f)
                                offset += pan
                            }
                        }
                ) {
                    if (points.size < 2) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "🗺️",
                                    fontSize = 40.sp
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = L10n.t("暂无足够足迹数据", "Not enough track points yet"),
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = L10n.t("在开启轨迹记录后，移动超过 5 米将自动绘制路径", "Move > 5m with track logging enabled to draw path"),
                                    fontSize = 11.sp,
                                    color = Color(0xFF7A7E88)
                                )
                            }
                        }
                    } else {
                        TrackCanvas(
                            points = points,
                            scale = scale,
                            offset = offset
                        )

                        // Map overlay controls (Reset zoom)
                        SmallFloatingActionButton(
                            onClick = {
                                scale = 1f
                                offset = Offset.Zero
                            },
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(12.dp),
                            containerColor = Color(0xFF22262E),
                            contentColor = Color.White
                        ) {
                            Text("↺", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Bottom Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        modifier = Modifier.weight(1f),
                        onClick = onExportGpx,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFFE500),
                            contentColor = Color.Black
                        )
                    ) {
                        Text(L10n.t("📤 导出 GPX 轨迹", "📤 Export GPX"), fontWeight = FontWeight.Bold)
                    }

                    if (points.isNotEmpty()) {
                        val last = points.last()
                        OutlinedButton(
                            onClick = {
                                val uri = Uri.parse("geo:${last.latitude},${last.longitude}?q=${last.latitude},${last.longitude}(Nikon Photo)")
                                val mapIntent = Intent(Intent.ACTION_VIEW, uri)
                                context.startActivity(Intent.createChooser(mapIntent, L10n.t("在地图中查看", "Open in Maps")))
                            },
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color.White
                            )
                        ) {
                            Text(L10n.t("外部地图", "Maps"))
                        }
                    }

                    OutlinedButton(
                        onClick = onClearTrack,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFFFF5252)
                        )
                    ) {
                        Text(L10n.t("清空", "Clear"))
                    }
                }
            }
        }
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, fontSize = 10.sp, color = Color(0xFF8E929B))
        Text(text = value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
    }
}

@Composable
private fun TrackCanvas(
    points: List<TrackPoint>,
    scale: Float,
    offset: Offset
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        if (width <= 0 || height <= 0 || points.size < 2) return@Canvas

        // Draw technical dark background grid
        val gridStep = 40.dp.toPx()
        var x = 0f
        while (x < width) {
            drawLine(
                color = Color(0xFF1A1C22),
                start = Offset(x, 0f),
                end = Offset(x, height),
                strokeWidth = 1f
            )
            x += gridStep
        }
        var y = 0f
        while (y < height) {
            drawLine(
                color = Color(0xFF1A1C22),
                start = Offset(0f, y),
                end = Offset(width, y),
                strokeWidth = 1f
            )
            y += gridStep
        }

        // Calculate bounding box
        var minLat = points[0].latitude
        var maxLat = points[0].latitude
        var minLon = points[0].longitude
        var maxLon = points[0].longitude

        for (p in points) {
            if (p.latitude < minLat) minLat = p.latitude
            if (p.latitude > maxLat) maxLat = p.latitude
            if (p.longitude < minLon) minLon = p.longitude
            if (p.longitude > maxLon) maxLon = p.longitude
        }

        val latSpan = max(maxLat - minLat, 0.0001)
        val midLat = (maxLat + minLat) / 2.0
        val lonScale = cos(Math.toRadians(midLat))
        val lonSpan = max((maxLon - minLon) * lonScale, 0.0001)

        val padding = 40f
        val drawW = width - padding * 2
        val drawH = height - padding * 2

        val factor = min(drawW / lonSpan, drawH / latSpan).toFloat()

        fun project(p: TrackPoint): Offset {
            val px = padding + ((p.longitude - minLon) * lonScale * factor).toFloat()
            val py = padding + ((maxLat - p.latitude) * factor).toFloat()
            // Apply gesture zoom & pan around canvas center
            val cx = width / 2f
            val cy = height / 2f
            val zx = (px - cx) * scale + cx + offset.x
            val zy = (py - cy) * scale + cy + offset.y
            return Offset(zx, zy)
        }

        // Draw track shadow / glow
        val path = Path()
        val p0 = project(points[0])
        path.moveTo(p0.x, p0.y)
        for (i in 1 until points.size) {
            val pt = project(points[i])
            path.lineTo(pt.x, pt.y)
        }

        // Outer glow
        drawPath(
            path = path,
            color = Color(0x33FFE500),
            style = Stroke(width = 12f * scale, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
        // Main polyline
        drawPath(
            path = path,
            color = Color(0xFFFFE500),
            style = Stroke(width = 4f * scale, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // Draw start point (Green pin)
        val startPos = project(points.first())
        drawCircle(
            color = Color(0xFF00E676),
            radius = 7f * scale,
            center = startPos
        )
        drawCircle(
            color = Color.White,
            radius = 3f * scale,
            center = startPos
        )

        // Draw end / current point (Red camera pin with halo)
        val endPos = project(points.last())
        drawCircle(
            color = Color(0x44FF1744),
            radius = 16f * scale,
            center = endPos
        )
        drawCircle(
            color = Color(0xFFFF1744),
            radius = 8f * scale,
            center = endPos
        )
        drawCircle(
            color = Color.White,
            radius = 3f * scale,
            center = endPos
        )
    }
}
