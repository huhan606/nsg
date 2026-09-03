package info.skyblond.nsp.service

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.core.content.ContextCompat
import info.skyblond.nsp.MainActivity
import info.skyblond.nsp.data.SettingsRepository
import info.skyblond.nsp.ui.L10n

/**
 * System Quick Settings Tile for fast camera connection toggling from Android's status bar.
 */
class NikonGpsTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        updateTileState()
    }

    override fun onClick() {
        super.onClick()
        val isConnected = CameraConnectionService.isRunning &&
            (CameraConnectionService.currentConnectionState is ConnectionState.Ready ||
             CameraConnectionService.currentConnectionState is ConnectionState.Busy)

        if (isConnected) {
            val intent = Intent(this, CameraConnectionService::class.java).apply {
                action = NotificationHelper.ACTION_DISCONNECT
            }
            startService(intent)
        } else {
            val repo = SettingsRepository(this)
            val cameras = repo.loadSavedCameras()
            if (cameras.isEmpty()) {
                val appIntent = Intent(this, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    val pending = PendingIntent.getActivity(this, 0, appIntent, PendingIntent.FLAG_IMMUTABLE)
                    startActivityAndCollapse(pending)
                } else {
                    @Suppress("DEPRECATION")
                    startActivityAndCollapse(appIntent)
                }
                return
            }

            ContextCompat.startForegroundService(
                this,
                Intent(this, CameraConnectionService::class.java)
            )
        }
        updateTileState()
    }

    private fun updateTileState() {
        val tile = qsTile ?: return
        val isRunning = CameraConnectionService.isRunning
        val state = CameraConnectionService.currentConnectionState

        val isActive = isRunning && (state is ConnectionState.Ready || state is ConnectionState.Busy)

        tile.state = if (isActive) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.label = L10n.t("尼康 GPS", "Nikon GPS")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.subtitle = when {
                state is ConnectionState.Ready -> L10n.t("已就绪", "Ready")
                state is ConnectionState.Busy -> L10n.t("同步中", "Syncing")
                state is ConnectionState.Connecting || state is ConnectionState.Scanning -> L10n.t("连接中", "Connecting")
                else -> L10n.t("未连接", "Disconnected")
            }
        }
        tile.updateTile()
    }
}
