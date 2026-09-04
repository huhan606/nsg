package info.skyblond.nsp.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import info.skyblond.nsp.MainActivity
import info.skyblond.nsp.R
import info.skyblond.nsp.ui.L10n

/**
 * Builds and updates the Live Activity / Lockscreen Capsule foreground-service notification.
 */
object NotificationHelper {
    const val CHANNEL_ID = "camera_connection_channel"
    const val NOTIFICATION_ID = 1
    const val ACTION_DISCONNECT = "info.skyblond.nsp.DISCONNECT"
    const val ACTION_SEND_GEO = "info.skyblond.nsp.SEND_GEO"

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                L10n.t("相机连接与实时定位", "Camera Connection & Live GPS"),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = L10n.t("保持与尼康相机的连接并实时呈现定位胶囊", "Maintains connection with Nikon camera and presents live GPS capsule")
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    fun build(
        context: Context,
        statusText: String,
        canSendGeo: Boolean = false,
        cameraName: String? = null,
        telemetryText: String? = null,
        satelliteText: String? = null
    ): Notification {
        val contentIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val disconnectIntent = PendingIntent.getService(
            context,
            1,
            Intent(context, CameraConnectionService::class.java).apply {
                action = ACTION_DISCONNECT
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val effectiveCam = cameraName?.takeIf { it.isNotBlank() } ?: L10n.t("尼康相机", "Nikon Camera")
        val capsuleTitle = "📷 $effectiveCam · $statusText"

        val bodyLine = buildString {
            if (!telemetryText.isNullOrBlank()) {
                append(telemetryText)
            } else {
                append(if (canSendGeo) L10n.t("GPS 已就绪 · 定位注入中", "GPS Ready · Syncing") else statusText)
            }
            if (!satelliteText.isNullOrBlank()) {
                append(" · ").append(satelliteText)
            }
        }

        val bigText = buildString {
            appendLine(capsuleTitle)
            appendLine(bodyLine)
            if (canSendGeo) {
                appendLine(L10n.t("持续向相机机身注入地理坐标 EXIF 信息", "Injecting real-time geographic coordinates into photo EXIF"))
            } else {
                appendLine(L10n.t("等待相机连接...", "Waiting for camera connection..."))
            }
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(capsuleTitle)
            .setContentText(bodyLine)
            .setSubText(L10n.t("实时定位胶囊", "Live Activity"))
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setColor(0xFFFFE500.toInt()) // Nikon Yellow accent
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC) // Visible on lockscreen
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
            .setOnlyAlertOnce(true)

        if (canSendGeo) {
            val sendGeoIntent = PendingIntent.getService(
                context,
                2,
                Intent(context, CameraConnectionService::class.java).apply {
                    action = ACTION_SEND_GEO
                },
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            builder.addAction(
                R.drawable.ic_launcher_foreground,
                L10n.t("⚡ 立即同步", "⚡ Sync Now"),
                sendGeoIntent
            )
        }

        builder.addAction(
            R.drawable.ic_launcher_foreground,
            L10n.t("⏏️ 断开", "⏏️ Disconnect"),
            disconnectIntent
        )

        return builder.build()
    }

    fun update(
        context: Context,
        statusText: String,
        canSendGeo: Boolean = false,
        cameraName: String? = null,
        telemetryText: String? = null,
        satelliteText: String? = null
    ) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, build(context, statusText, canSendGeo, cameraName, telemetryText, satelliteText))
    }
}
