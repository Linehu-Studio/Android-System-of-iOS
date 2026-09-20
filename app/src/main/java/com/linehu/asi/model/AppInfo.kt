package com.linehu.asi.model

import android.content.ComponentName

/**
 * A launchable Android application installed on the device.
 * [key] is the stable identity used across layout persistence & caches:
 * `packageName/className`.
 */
data class AppInfo(
    val key: String,
    val label: String,
    val componentName: ComponentName,
) {
    constructor(label: String, componentName: ComponentName) : this(
        key = "${componentName.packageName}/${componentName.className}",
        label = label,
        componentName = componentName,
    )
}
