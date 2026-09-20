package com.linehu.asi.system

import android.content.Context
import android.media.AudioManager
import android.provider.Settings
import android.view.WindowManager

/**
 * Brightness + volume for the control center. Brightness writes need the
 * WRITE_SETTINGS special grant — [canWriteBrightness] tells the UI whether
 * to show the slider or a "grant access" hint.
 */
class BrightnessController(private val context: Context) {

    fun canWriteBrightness(): Boolean = Settings.System.canWrite(context)

    /** Current brightness 0..1 (system adaptive brightness may override). */
    fun brightness(): Float = try {
        val raw = Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS)
        raw / 255f
    } catch (t: Throwable) {
        0.5f
    }

    fun setBrightness(fraction: Float) {
        if (!canWriteBrightness()) return
        runCatching {
            Settings.System.putInt(
                context.contentResolver,
                Settings.System.SCREEN_BRIGHTNESS_MODE,
                Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL,
            )
            Settings.System.putInt(
                context.contentResolver,
                Settings.System.SCREEN_BRIGHTNESS,
                (fraction.coerceIn(0.02f, 1f) * 255).toInt(),
            )
        }
        runCatching {
            val lp = (context as? android.app.Activity)?.window?.attributes
            lp?.screenBrightness = fraction.coerceIn(0.02f, 1f)
            (context as? android.app.Activity)?.window?.attributes = lp
        }
    }
}

class VolumeController(private val context: Context) {
    private val audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    fun volume(): Float {
        val max = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        return audio.getStreamVolume(AudioManager.STREAM_MUSIC) / max.toFloat()
    }

    fun setVolume(fraction: Float) {
        val max = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        runCatching {
            audio.setStreamVolume(
                AudioManager.STREAM_MUSIC,
                (fraction * max).toInt().coerceIn(0, max),
                0,
            )
        }
    }
}
