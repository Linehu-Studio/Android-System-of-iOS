package com.linehu.asi.core.designsystem

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * iOS-style haptic vocabulary backed by Vibrator primitives (API 29+)
 * with a plain-jolt fallback below.
 */
class Haptics(context: Context) {
    private val vibrator: Vibrator = if (Build.VERSION.SDK_INT >= 31) {
        (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    private fun primitive(id: Int, fallbackMs: Long) {
        if (Build.VERSION.SDK_INT >= 29) {
            vibrator.vibrate(VibrationEffect.createPredefined(id))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(fallbackMs)
        }
    }

    /** Light tap — icon press, toggle. */
    fun tap() = primitive(VibrationEffect.EFFECT_CLICK, 12)

    /** Medium — open folder, control center settle. */
    fun impact() = primitive(VibrationEffect.EFFECT_HEAVY_CLICK, 22)

    /** Success — unlock, drag settled. */
    fun success() = primitive(VibrationEffect.EFFECT_CLICK, 15)

    /** Warning-ish double jolt — delete confirm. */
    fun warning() = primitive(VibrationEffect.EFFECT_DOUBLE_CLICK, 40)
}
