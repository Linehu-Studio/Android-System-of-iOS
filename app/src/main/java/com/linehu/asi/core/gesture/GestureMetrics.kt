package com.linehu.asi.core.gesture

import androidx.compose.ui.unit.dp

/** Central tuning knobs for shell gestures (values in dp / dp-per-second). */
object GestureMetrics {
    /** Home-indicator pill size. */
    val indicatorWidth = 134.dp
    val indicatorHeight = 5.dp
    val indicatorBottomMargin = 6.dp

    /** Bottom strip that captures the swipe-up gesture. */
    val gestureZoneHeight = 40.dp

    /** Minimum upward fling velocity that always goes home. */
    val homeFlingVelocity = 2200f

    /** Drag fraction of screen height that commits to home on release. */
    const val homeDragFraction = 0.18f

    /** Pause duration while past the switcher threshold that triggers the switcher. */
    const val switcherPauseMs = 180L

    /** Drag (px) past which the swipe-up hold means "show the switcher". */
    const val switcherThresholdPx = 120f

    /** Lock screen rubber-banding: only 55% of the drag moves the screen. */
    const val lockScreenDragDamp = 0.55f

    /** Lock screen unlock commit fraction of screen height. */
    const val unlockFraction = 0.22f
}
