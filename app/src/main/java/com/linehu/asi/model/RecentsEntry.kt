package com.linehu.asi.model

/** One card in the app switcher. */
data class RecentsEntry(
    val appKey: String,
    val label: String,
    val lastUsed: Long,
    /** True when this entry comes from UsageStats, not our own history. */
    val fromSystemStats: Boolean = false,
)
