package info.skyblond.nsp.util

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import info.skyblond.nsp.data.SettingsRepository

/**
 * Provides tactile haptic feedback for key camera connection and GPS events.
 */
object HapticHelper {

    @Suppress("DEPRECATION")
    private fun getVibrator(context: Context): Vibrator? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            manager?.defaultVibrator
        } else {
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    /**
     * Subtle double-pulse buzz confirming camera connection is established and ready.
     */
    fun vibrateConnectSuccess(context: Context) {
        val repo = SettingsRepository(context)
        if (!repo.isHapticFeedbackEnabled()) return
        val vibrator = getVibrator(context) ?: return
        if (!vibrator.hasVibrator()) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val timings = longArrayOf(0, 45, 60, 45)
            val amplitudes = intArrayOf(0, 180, 0, 240)
            val effect = VibrationEffect.createWaveform(timings, amplitudes, -1)
            vibrator.vibrate(effect)
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(longArrayOf(0, 45, 60, 45), -1)
        }
    }

    /**
     * Subtle tick confirming GPS coordinates were sent to the camera.
     */
    fun vibrateGeoSent(context: Context) {
        val repo = SettingsRepository(context)
        if (!repo.isHapticFeedbackEnabled()) return
        val vibrator = getVibrator(context) ?: return
        if (!vibrator.hasVibrator()) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(20, 100))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(20)
        }
    }

    /**
     * Single warning pulse when camera disconnects unexpectedly or manually.
     */
    fun vibrateDisconnect(context: Context) {
        val repo = SettingsRepository(context)
        if (!repo.isHapticFeedbackEnabled()) return
        val vibrator = getVibrator(context) ?: return
        if (!vibrator.hasVibrator()) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(100)
        }
    }
}
