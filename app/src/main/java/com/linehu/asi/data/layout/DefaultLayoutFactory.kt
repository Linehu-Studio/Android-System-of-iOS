package com.linehu.asi.data.layout

import com.linehu.asi.model.AppId
import com.linehu.asi.model.GridEntry
import com.linehu.asi.model.HomeLayout

/**
 * Builds the first-run layout: a sensible dock (dialer/messages/browser/camera
 * when present, built-ins otherwise) and all remaining apps paged
 * alphabetically after the built-in apps on page one.
 */
object DefaultLayoutFactory {

    private const val DOCK_SIZE = 4
    private const val CELLS_PER_PAGE = 24

    fun create(installedApps: List<com.linehu.asi.model.AppInfo>): HomeLayout {
        val byKey = installedApps.associateBy { it.key }

        fun find(vararg keys: String): GridEntry.AppEntry? =
            keys.firstNotNullOfOrNull { k -> byKey[k]?.let { GridEntry.AppEntry(it.key) } }

        val dockCandidates = listOf(
            arrayOf("com.android.dialer/com.android.dialer.main.impl.MainActivity"),
            arrayOf(
                "com.google.android.dialer/com.android.dialer.main.impl.MainActivity",
                "com.android.dialer/com.android.dialer.DialtactsActivity",
            ),
            arrayOf(
                "com.google.android.apps.messaging/.ui.ConversationListActivity",
                "com.android.mms/.ui.ConversationList",
            ),
            arrayOf(
                "com.android.chrome/com.google.android.apps.chrome.Main",
                "org.mozilla.firefox/.App",
                "com.android.browser/.BrowserActivity",
            ),
            arrayOf(
                "com.android.camera2/com.android.camera.CameraLauncher",
                "com.google.android.GoogleCamera/com.android.camera.CameraLauncher",
            ),
        ).mapNotNull { find(*it) }

        val dock: MutableList<GridEntry> = dockCandidates.take(DOCK_SIZE).toMutableList()
        val builtins = listOf(
            AppId.SETTINGS,
            AppId.CALCULATOR,
            AppId.CLOCK,
            AppId.NOTES,
            AppId.WEATHER,
            AppId.PHOTOS,
        )
        val builtinEntries = builtins.map { GridEntry.BuiltinEntry(it) }
        for (b in builtinEntries) {
            if (dock.size >= DOCK_SIZE) break
            dock.add(b)
        }

        val dockedKeys = dock.mapNotNull { (it as? GridEntry.AppEntry)?.key }.toSet()
        val rest = installedApps
            .filter { it.key !in dockedKeys }
            .sortedWith(compareByDescending<com.linehu.asi.model.AppInfo> { isChinese(it.label) }.thenBy { it.label })
            .map { GridEntry.AppEntry(it.key) }

        val firstPage = (builtinEntries + rest).take(CELLS_PER_PAGE)
        val remaining = (builtinEntries + rest).drop(CELLS_PER_PAGE)
        val pages = mutableListOf(firstPage)
        var cursor = 0
        while (cursor < remaining.size) {
            pages += remaining.subList(cursor, minOf(cursor + CELLS_PER_PAGE, remaining.size)).toList()
            cursor += CELLS_PER_PAGE
        }
        return HomeLayout(dock = dock, pages = pages)
    }

    private fun isChinese(label: String): Boolean =
        label.isNotEmpty() && label[0] in '一'..'鿿'
}
