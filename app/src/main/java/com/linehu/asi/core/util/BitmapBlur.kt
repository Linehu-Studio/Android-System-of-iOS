package com.linehu.asi.core.util

import android.graphics.Bitmap
import kotlin.math.max
import kotlin.math.min

/**
 * Cheap, dependency-free blur: downscale with bilinear filtering, then
 * upscale — two passes make it smooth enough to pass for "frosted glass"
 * when combined with a translucent scrim.
 */
fun Bitmap.scaledBlur(targetWidth: Int = 96, passes: Int = 2): Bitmap {
    var current = this
    val w = max(1, min(targetWidth, width))
    val h = max(1, (w.toLong() * height / max(1, width)).toInt())
    var small = Bitmap.createScaledBitmap(current, w, h, true)
    repeat(passes) {
        // Ping-pong between the small size and half of it to smear pixels.
        val w2 = max(1, w / 2)
        val h2 = max(1, h / 2)
        val half = Bitmap.createScaledBitmap(small, w2, h2, true)
        small = Bitmap.createScaledBitmap(half, w, h, true)
        if (half != small && half != this) half.recycle()
    }
    if (current != this) current.recycle()
    return small
}
