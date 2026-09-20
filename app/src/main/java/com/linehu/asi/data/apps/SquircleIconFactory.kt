package com.linehu.asi.data.apps

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * Renders an Android app icon into an iOS-style squircle bitmap.
 *
 * Adaptive icons keep their own layers (background fills, foreground is
 * centered at the visible-zone ratio). Legacy icons get a light backing card.
 */
object SquircleIconFactory {

    fun render(context: Context, component: ComponentName, sizePx: Int): Bitmap? {
        val pm = context.packageManager
        val drawable = runCatching { pm.getActivityIcon(component) }.getOrNull() ?: return null
        return renderDrawable(drawable, sizePx)
    }

    fun renderDrawable(drawable: Drawable, sizePx: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.save()
        // Platform twin of core.designsystem.squirclePath (superellipse n=5).
        val clip = android.graphics.Path().apply {
            val a = sizePx / 2f
            val e = 2f / 5f
            val steps = 96
            for (i in 0..steps) {
                val theta = (i.toDouble() / steps) * 2.0 * Math.PI
                val c = kotlin.math.cos(theta).toFloat()
                val s = kotlin.math.sin(theta).toFloat()
                val x = a * (if (c < 0) -1f else 1f) * kotlin.math.abs(c).pow(e) + a
                val y = a * (if (s < 0) -1f else 1f) * kotlin.math.abs(s).pow(e) + a
                if (i == 0) moveTo(x, y) else lineTo(x, y)
            }
            close()
        }
        canvas.clipPath(clip)

        when (drawable) {
            is AdaptiveIconDrawable -> {
                // Visible zone of an adaptive icon is 72/108 of the canvas.
                val visible = (sizePx * 72f / 108f).roundToInt()
                val inset = (sizePx - visible) / 2f
                drawable.background?.let { bg ->
                    bg.setBounds(0, 0, sizePx, sizePx)
                    bg.draw(canvas)
                } ?: canvas.drawColor(android.graphics.Color.WHITE)
                drawable.foreground?.let { fg ->
                    fg.setBounds(inset.toInt(), inset.toInt(), (inset + visible).toInt(), (inset + visible).toInt())
                    fg.draw(canvas)
                }
            }
            else -> {
                canvas.drawColor(android.graphics.Color.WHITE)
                val pad = (sizePx * 0.08f).roundToInt()
                drawable.setBounds(pad, pad, sizePx - pad, sizePx - pad)
                drawable.draw(canvas)
            }
        }
        canvas.restore()
        return bitmap
    }
}
