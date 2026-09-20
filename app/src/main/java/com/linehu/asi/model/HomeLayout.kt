package com.linehu.asi.model

/**
 * UI-facing home screen model.
 *
 * The grid is a fixed 4×6 per page (parameterized elsewhere); the dock is a
 * separate 4-slot strip. Folder children may only be apps/builtins — no
 * nested folders, matching iOS.
 */
sealed interface GridEntry {
    val stableId: String

    /** External Android app. */
    data class AppEntry(val key: String) : GridEntry {
        override val stableId: String get() = "app:$key"
    }

    /** Built-in ASI app scene. */
    data class BuiltinEntry(val appId: AppId) : GridEntry {
        override val stableId: String get() = "builtin:${appId.name}"
    }

    data class FolderEntry(
        val id: String,
        val title: String,
        val children: List<GridEntry>,
    ) : GridEntry {
        override val stableId: String get() = "folder:$id"
    }
}

data class HomeLayout(
    val dock: List<GridEntry>,
    val pages: List<List<GridEntry>>,
) {
    companion object {
        val EMPTY = HomeLayout(dock = emptyList(), pages = listOf(emptyList()))
    }
}
