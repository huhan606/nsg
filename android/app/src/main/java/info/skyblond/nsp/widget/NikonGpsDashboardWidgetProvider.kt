package info.skyblond.nsp.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import androidx.core.content.ContextCompat
import info.skyblond.nsp.MainActivity
import info.skyblond.nsp.R
import info.skyblond.nsp.data.SettingsRepository
import info.skyblond.nsp.service.CameraConnectionService
import info.skyblond.nsp.service.ConnectionState
import info.skyblond.nsp.service.GpxTrackLogger
import info.skyblond.nsp.service.NotificationHelper
import info.skyblond.nsp.ui.L10n
import java.util.Locale

/**
 * 4x4 Android Home Screen Dashboard Widget for comprehensive photography monitoring,
 * live telemetry, satellite radar, footprint statistics, and quick controls.
 */
class NikonGpsDashboardWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            ACTION_TOGGLE_CONNECT -> {
                handleToggleConnect(context)
            }
            ACTION_SEND_GEO -> {
                handleSendGeo(context)
            }
            ACTION_UPDATE_ALL -> {
                updateAllWidgets(context)
            }
        }
    }

    private fun handleToggleConnect(context: Context) {
        val isConnected = CameraConnectionService.isRunning &&
            (CameraConnectionService.currentConnectionState is ConnectionState.Ready ||
             CameraConnectionService.currentConnectionState is ConnectionState.Busy)

        try {
            if (isConnected) {
                val disconnectIntent = Intent(context, CameraConnectionService::class.java).apply {
                    action = NotificationHelper.ACTION_DISCONNECT
                }
                context.startService(disconnectIntent)
            } else {
                val repo = SettingsRepository(context)
                val cameras = repo.loadSavedCameras()
                if (cameras.isEmpty()) {
                    val appIntent = Intent(context, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    }
                    context.startActivity(appIntent)
                    return
                }

                val defaultName = repo.defaultConnectCameraName()
                val targetCam = cameras.firstOrNull { it.name == defaultName } ?: cameras.firstOrNull()
                val connectIntent = Intent(context, CameraConnectionService::class.java).apply {
                    action = CameraConnectionService.ACTION_CONNECT
                    if (targetCam != null) {
                        putExtra(CameraConnectionService.EXTRA_CAMERA_NAME, targetCam.name)
                    }
                }
                ContextCompat.startForegroundService(context, connectIntent)
            }
        } catch (e: Exception) {
            android.util.Log.e("NikonGps4x4Widget", "Failed to toggle connect: ${e.message}", e)
            try {
                val appIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                context.startActivity(appIntent)
            } catch (_: Exception) {}
        }
        updateAllWidgets(context)
    }

    private fun handleSendGeo(context: Context) {
        try {
            val sendGeoIntent = Intent(context, CameraConnectionService::class.java).apply {
                action = NotificationHelper.ACTION_SEND_GEO
            }
            context.startService(sendGeoIntent)
        } catch (e: Exception) {
            android.util.Log.e("NikonGps4x4Widget", "Failed to send geo from widget: ${e.message}", e)
        }
    }

    companion object {
        const val ACTION_TOGGLE_CONNECT = "info.skyblond.nsp.widget.4x4.TOGGLE_CONNECT"
        const val ACTION_SEND_GEO = "info.skyblond.nsp.widget.4x4.SEND_GEO"
        const val ACTION_UPDATE_ALL = "info.skyblond.nsp.widget.4x4.UPDATE_ALL"

        fun updateAllWidgets(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context) ?: return
            val thisWidget = ComponentName(context, NikonGpsDashboardWidgetProvider::class.java)
            val allWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget) ?: return
            for (widgetId in allWidgetIds) {
                updateAppWidget(context, appWidgetManager, widgetId)
            }
        }

        fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val views = RemoteViews(context.packageName, R.layout.widget_nikon_gps_4x4)

            val isRunning = CameraConnectionService.isRunning
            val state = CameraConnectionService.currentConnectionState
            val isConnected = isRunning && (state is ConnectionState.Ready || state is ConnectionState.Busy)
            val isConnecting = isRunning && (state is ConnectionState.Connecting ||
                state is ConnectionState.Scanning ||
                state is ConnectionState.Discovering ||
                state is ConnectionState.Pairing ||
                state is ConnectionState.Bonding)

            // Status label & color
            val (statusText, statusColor, syncStateDesc) = when {
                state is ConnectionState.Ready -> Triple(L10n.t("● 已就绪", "● Ready"), 0xFF3DDC84.toInt(), L10n.t("实时注入", "Injecting"))
                state is ConnectionState.Busy -> Triple(L10n.t("● 同步中", "● Syncing"), 0xFFFFB300.toInt(), L10n.t("传输中", "Syncing"))
                isConnecting -> Triple(L10n.t("● 连接中...", "● Connecting..."), 0xFF42A5F5.toInt(), L10n.t("搜索中", "Scanning"))
                state is ConnectionState.Error -> Triple(L10n.t("● 异常", "● Error"), 0xFFFF5252.toInt(), L10n.t("重试中", "Retrying"))
                else -> Triple(L10n.t("● 未连接", "● Disconnected"), 0xFF9E9E9E.toInt(), L10n.t("待命", "Standby"))
            }
            views.setTextViewText(R.id.widget_4x4_status_text, statusText)
            views.setTextColor(R.id.widget_4x4_status_text, statusColor)
            views.setTextViewText(R.id.widget_4x4_sync_state, syncStateDesc)

            // Camera Name
            val repo = SettingsRepository(context)
            val savedCameras = repo.loadSavedCameras()
            val defaultName = repo.defaultConnectCameraName()
            val targetCam = savedCameras.firstOrNull { it.name == defaultName } ?: savedCameras.firstOrNull()
            val camDisplay = targetCam?.displayName ?: L10n.t("未保存相机", "No camera saved")
            views.setTextViewText(R.id.widget_4x4_camera_name, "📷 $camDisplay")

            // Telemetry / Coordinates & Altitude
            val coordsText = CameraConnectionService.lastFormattedLocation
                ?: if (isConnected) L10n.t("GPS 已就绪 · 等待有效定位", "GPS Ready · Waiting fix")
                else L10n.t("待命 · 点击下方一键重连", "Standby · Tap connect below")
            views.setTextViewText(R.id.widget_4x4_coords, coordsText)

            // Accuracy & Satellite Radar
            val satellitesInfo = CameraConnectionService.lastSatelliteInfo
            if (satellitesInfo != null) {
                views.setTextViewText(R.id.widget_4x4_satellites, "🛰️ $satellitesInfo")
            } else {
                views.setTextViewText(R.id.widget_4x4_satellites, L10n.t("🛰️ 搜星: 卫星多频段解算待命中", "🛰️ Satellites: GNSS receiver on standby"))
            }

            // Accuracy
            val accuracyText = if (isConnected) L10n.t("高精度双频", "High Precision") else "--"
            views.setTextViewText(R.id.widget_4x4_accuracy, L10n.t("精度: ", "Accuracy: ") + accuracyText)

            // Footprint Track stats
            val trackSummary = GpxTrackLogger.getTrackSummary()
            val pointCount = trackSummary?.pointCount ?: GpxTrackLogger.pointCount()
            val distKm = if (trackSummary != null) trackSummary.totalDistanceMeters / 1000.0 else 0.0
            views.setTextViewText(R.id.widget_4x4_track_points, "$pointCount " + L10n.t("点", "pts"))
            views.setTextViewText(R.id.widget_4x4_track_distance, String.format(Locale.US, "%.2f km", distKm))
            views.setTextViewText(R.id.widget_4x4_sync_interval, "${repo.gpsIntervalSeconds()}" + L10n.t("秒", "s"))

            // Action Button 1: Connect / Disconnect
            val actionBtnText = when {
                isConnected -> L10n.t("断开连接", "Disconnect")
                isConnecting -> L10n.t("连接中...", "Connecting...")
                else -> L10n.t("一键连接", "Connect")
            }
            views.setTextViewText(R.id.widget_4x4_btn_connect, actionBtnText)

            val toggleIntent = Intent(context, NikonGpsDashboardWidgetProvider::class.java).apply {
                action = ACTION_TOGGLE_CONNECT
            }
            val togglePending = PendingIntent.getBroadcast(
                context,
                2001,
                toggleIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_4x4_btn_connect, togglePending)

            // Action Button 2: Sync Now
            val syncIntent = Intent(context, NikonGpsDashboardWidgetProvider::class.java).apply {
                action = ACTION_SEND_GEO
            }
            val syncPending = PendingIntent.getBroadcast(
                context,
                2002,
                syncIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_4x4_btn_sync, syncPending)

            // Action Button 3: Track Map
            val mapIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(MainActivity.EXTRA_OPEN_TRACK_MAP, true)
            }
            val mapPending = PendingIntent.getActivity(
                context,
                2003,
                mapIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_4x4_btn_map, mapPending)

            // Action Button 4 & Card Root: Open App
            val openIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val openPending = PendingIntent.getActivity(
                context,
                2004,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_4x4_btn_open, openPending)
            views.setOnClickPendingIntent(R.id.widget_4x4_root, openPending)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
