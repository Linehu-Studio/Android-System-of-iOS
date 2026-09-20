package com.linehu.asi.system

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Rect
import android.os.Build
import android.util.DisplayMetrics
import android.view.WindowManager
import com.linehu.asi.data.settings.FreeformMode

/**
 * Decides whether external apps launch into freeform windows (Stage-Manager
 * style) or plain fullscreen, and computes the default window rect.
 *
 * Freeform availability is genuinely fragmented: it depends on the ROM
 * (DeX, tablets, some Chinese ROMs, or the "force resizable" developer
 * option). We probe the feature flag, honor the user override, and always
 * let the actual launch fall back silently (see [AppLauncher]).
 */
class FreeformManager(private val context: Context) {

    private val freeformSupported: Boolean
        get() = Build.VERSION.SDK_INT >= 30 &&
            context.packageManager.hasSystemFeature(PackageManager.FEATURE_FREEFORM_WINDOW_MANAGEMENT)

    fun shouldLaunchFreeform(mode: FreeformMode = FreeformMode.AUTO): Boolean = when (mode) {
        FreeformMode.ON -> Build.VERSION.SDK_INT >= 30
        FreeformMode.OFF -> false
        FreeformMode.AUTO -> freeformSupported
    }

    /** A generous iPad-like window occupying most of the screen. */
    fun defaultWindowBounds(): Rect {
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val bounds = wm.currentWindowMetrics.bounds
        val marginX = (bounds.width() * 0.06f).toInt()
        val marginY = (bounds.height() * 0.10f).toInt()
        return Rect(
            bounds.left + marginX,
            bounds.top + marginY,
            bounds.right - marginX,
            bounds.bottom - marginY,
        )
    }
}
