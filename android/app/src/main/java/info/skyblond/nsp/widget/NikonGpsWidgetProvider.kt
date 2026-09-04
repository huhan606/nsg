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
import info.skyblond.nsp.service.NotificationHelper
import info.skyblond.nsp.ui.L10n

/**
 * Android Home Screen Widget for quick status monitoring and 1-tap connect/disconnect.
 */
class NikonGpsWidgetProvider : AppWidgetProvider() {

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
            android.util.Log.e("NikonGpsWidget", "Failed to toggle connect from widget: ${e.message}", e)
            try {
                val appIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                context.startActivity(appIntent)
            } catch (_: Exception) {}
        }
        updateAllWidgets(context)
    }

    companion object {
        const val ACTION_TOGGLE_CONNECT = "info.skyblond.nsp.widget.TOGGLE_CONNECT"
        const val ACTION_UPDATE_ALL = "info.skyblond.nsp.widget.UPDATE_ALL"

        fun updateAllWidgets(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context) ?: return
            val thisWidget = ComponentName(context, NikonGpsWidgetProvider::class.java)
            val allWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget) ?: return
            for (widgetId in allWidgetIds) {
                updateAppWidget(context, appWidgetManager, widgetId)
            }
        }

        fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val views = RemoteViews(context.packageName, R.layout.widget_nikon_gps)

            val isRunning = CameraConnectionService.isRunning
            val state = CameraConnectionService.currentConnectionState
            val isConnected = isRunning && (state is ConnectionState.Ready || state is ConnectionState.Busy)
            val isConnecting = isRunning && (state is ConnectionState.Connecting ||
                state is ConnectionState.Scanning ||
                state is ConnectionState.Discovering ||
                state is ConnectionState.Pairing ||
                state is ConnectionState.Bonding)

            // Status label & color
            val (statusText, statusColor) = when {
                state is ConnectionState.Ready -> Pair(L10n.t("● 已就绪", "● Ready"), 0xFF3DDC84.toInt())
                state is ConnectionState.Busy -> Pair(L10n.t("● 同步中", "● Syncing"), 0xFFFFB300.toInt())
                isConnecting -> Pair(L10n.t("● 连接中...", "● Connecting..."), 0xFF42A5F5.toInt())
                state is ConnectionState.Error -> Pair(L10n.t("● 异常", "● Error"), 0xFFFF5252.toInt())
                else -> Pair(L10n.t("● 未连接", "● Disconnected"), 0xFF9E9E9E.toInt())
            }
            views.setTextViewText(R.id.widget_status_text, statusText)
            views.setTextColor(R.id.widget_status_text, statusColor)

            // Camera Name
            val repo = SettingsRepository(context)
            val savedCameras = repo.loadSavedCameras()
            val defaultName = repo.defaultConnectCameraName()
            val targetCam = savedCameras.firstOrNull { it.name == defaultName } ?: savedCameras.firstOrNull()
            val camDisplay = targetCam?.displayName ?: L10n.t("未保存相机", "No camera saved")
            views.setTextViewText(R.id.widget_camera_name, "📷 $camDisplay")

            // Telemetry / Subtitle
            val telemetry = CameraConnectionService.lastFormattedLocation
                ?: if (isConnected) L10n.t("GPS 已就绪 · 定位同步中", "GPS Ready · Syncing fix")
                else L10n.t("待命 · 点击一键重连", "Standby · Tap to reconnect")
            views.setTextViewText(R.id.widget_telemetry_text, telemetry)

            // Primary Action Button text
            val actionBtnText = when {
                isConnected -> L10n.t("断开连接", "Disconnect")
                isConnecting -> L10n.t("连接中...", "Connecting...")
                else -> L10n.t("一键连接", "Connect")
            }
            views.setTextViewText(R.id.widget_btn_action, actionBtnText)

            // PendingIntent for action button
            val toggleIntent = Intent(context, NikonGpsWidgetProvider::class.java).apply {
                action = ACTION_TOGGLE_CONNECT
            }
            val togglePending = PendingIntent.getBroadcast(
                context,
                1001,
                toggleIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_btn_action, togglePending)

            // PendingIntent for Open App button
            val openIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val openPending = PendingIntent.getActivity(
                context,
                1002,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_btn_open, openPending)
            views.setOnClickPendingIntent(R.id.widget_root, openPending)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
