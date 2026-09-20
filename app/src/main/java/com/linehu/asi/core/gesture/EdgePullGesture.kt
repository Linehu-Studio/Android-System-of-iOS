package com.linehu.asi.core.gesture

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput

/**
 * Top-edge pull detector for the two iOS "centers". The drag begins in the
 * strip this modifier is attached to; each downward pixel reports a fraction
 * of the strip height so the overlay can track the finger; on release the
 * fraction + fling velocity decide open vs. dismiss.
 *
 * Note: the very top pixels of the screen belong to the system notification
 * shade — attach this strip *below* the status bar and it works everywhere;
 * AssistiveTouch and wallpaper long-press remain as guaranteed entries.
 */
fun Modifier.edgePull(
    enabled: Boolean = true,
    onPull: (fraction: Float) -> Unit,
    onRelease: (fraction: Float, velocity: Float) -> Unit,
    commitFraction: Float = 0.35f,
): Modifier = pointerInput(enabled) {
    if (!enabled) return@pointerInput
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        var total = 0f
        var lastY = down.position.y
        var lastTime = System.nanoTime()
        var velocity = 0f
        val result = drag(down.id) { change ->
            val dy = change.position.y - lastY
            val now = System.nanoTime()
            val dt = ((now - lastTime) / 1_000_000f).coerceAtLeast(1f)
            velocity = velocity * 0.6f + (dy / dt * 1000f) * 0.4f // px/s, smoothed
            lastTime = now
            lastY = change.position.y
            if (dy > 0 || total > 0) {
                total = (total + dy).coerceAtLeast(0f)
                onPull(total / size.height)
                change.consume()
            }
        }
        // If drag() returned early (cancellation), treat as release anyway.
        onRelease(total / size.height, velocity)
    }
}
