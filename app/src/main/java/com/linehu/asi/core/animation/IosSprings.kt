package com.linehu.asi.core.animation

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween

/**
 * Central place for iOS-feel motion specs so every surface in the app
 * springs the same way. Tuned to approximate UIKit's default damping.
 */
object IosSprings {
    /** Snappy, no overshoot — used for scene switches, panel settles. */
    fun <T> snappy(): androidx.compose.animation.core.SpringSpec<T> = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = 500f,
    )

    /** Slight overshoot — used for Dynamic Island morphs, folder open. */
    fun <T> bouncy(): androidx.compose.animation.core.SpringSpec<T> = spring(
        dampingRatio = 0.78f,
        stiffness = 420f,
    )

    /** Soft, used for overlay reveals when velocity is unknown. */
    fun <T> soft(): androidx.compose.animation.core.SpringSpec<T> = spring(
        dampingRatio = 0.9f,
        stiffness = 220f,
    )

    /** Standard iOS fade. */
    fun <T> fade(): androidx.compose.animation.core.TweenSpec<T> = tween(220)
}
