package com.linehu.asi.feature.springboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.linehu.asi.data.apps.AppRepository
import com.linehu.asi.data.apps.IconCache
import com.linehu.asi.data.layout.DefaultLayoutFactory
import com.linehu.asi.data.layout.LayoutRepository
import com.linehu.asi.data.settings.AsiSettings
import com.linehu.asi.data.settings.SettingsRepository
import com.linehu.asi.model.AppId
import com.linehu.asi.model.AppInfo
import com.linehu.asi.model.GridEntry
import com.linehu.asi.model.HomeLayout
import com.linehu.asi.system.AppLauncher
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SpringboardState(
    val layout: HomeLayout = HomeLayout.EMPTY,
    val appsByKey: Map<String, AppInfo> = emptyMap(),
)

/**
 * Drives the home grid: merges the persisted layout with the installed-app
 * set, keeps both in sync when packages come and go, and forwards open
 * events (external apps go straight to the system, built-ins bubble up to
 * the shell).
 */
class SpringboardViewModel(
    private val appContext: android.content.Context,
    private val appRepository: AppRepository,
    private val layoutRepository: LayoutRepository,
    private val settingsRepository: SettingsRepository,
    private val appLauncher: AppLauncher,
    val iconCache: IconCache,
) : ViewModel() {

    private val layoutState = MutableStateFlow(HomeLayout.EMPTY)
    private val appsState = MutableStateFlow<Map<String, AppInfo>>(emptyMap())

    val state: StateFlow<SpringboardState> =
        combine(layoutState, appsState) { layout, apps ->
            SpringboardState(layout, apps)
        }.stateIn(viewModelScope, SharingStarted.Eagerly, SpringboardState())

    val settings: StateFlow<AsiSettings> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, AsiSettings())

    private val builtinOpens = MutableSharedFlow<AppId>(extraBufferCapacity = 8)
    /** Built-in app open requests — the shell scene machine consumes these. */
    val builtinOpenEvents: SharedFlow<AppId> = builtinOpens

    init {
        appRepository.start()
        viewModelScope.launch {
            appRepository.apps.collect { list -> appsState.value = list.associateBy { it.key } }
        }
        viewModelScope.launch {
            layoutRepository.layout.collect { layoutState.value = it }
        }
        viewModelScope.launch {
            // First run: wait until the package scan has produced something,
            // then persist a default arrangement.
            val apps = appRepository.apps.first { it.isNotEmpty() }
            layoutRepository.ensureInitialized(
                DefaultLayoutFactory.create(apps),
            )
        }
        viewModelScope.launch {
            appRepository.packageChanged.collect {
                appRepository.refresh()
                syncLayoutWithInstalledApps()
            }
        }
    }

    fun open(entry: GridEntry, onIslandFlash: (String) -> Unit = {}) {
        when (entry) {
            is GridEntry.AppEntry -> appsState.value[entry.key]?.let { app ->
                appLauncher.launch(app, settings.value.freeformMode, onIslandFlash)
            }
            is GridEntry.BuiltinEntry -> builtinOpens.tryEmit(entry.appId)
            is GridEntry.FolderEntry -> Unit // handled by the folder overlay in SpringboardScreen
        }
    }

    // ---- M5: editing (jiggle / drag / folders), debounced persistence ----

    private var pendingSave: kotlinx.coroutines.Job? = null

    /** Optimistic local layout update; persists after 500ms of quiet. */
    fun onLayoutEdited(newLayout: com.linehu.asi.model.HomeLayout) {
        layoutState.value = newLayout
        pendingSave?.cancel()
        pendingSave = viewModelScope.launch {
            kotlinx.coroutines.delay(500)
            layoutRepository.save(newLayout)
        }
    }

    /** A grid slot address: page -1 = dock. */
    data class Slot(val page: Int, val index: Int)

    fun moveEntry(from: Slot, to: Slot) {
        val layout = layoutState.value
        onLayoutEdited(moveSlot(layout, from, to))
    }

    /** Drop [from] onto an occupied [to] slot → merge into a new folder. */
    fun mergeIntoFolder(from: Slot, onto: Slot) {
        val layout = layoutState.value
        onLayoutEdited(mergeSlots(layout, from, onto))
    }

    fun removeAt(page: Int, index: Int) {
        val layout = layoutState.value
        onLayoutEdited(removeSlot(layout, page, index))
    }

    fun removeFromFolder(folderId: String, childIndex: Int) {
        val layout = layoutState.value
        onLayoutEdited(removeFolderChild(layout, folderId, childIndex))
    }

    fun renameFolder(folderId: String, title: String) {
        val layout = layoutState.value
        onLayoutEdited(
            layout.copy(
                pages = layout.pages.map { page ->
                    page.map { cell ->
                        if (cell is GridEntry.FolderEntry && cell.id == folderId) {
                            cell.copy(title = title.ifBlank { "文件夹" })
                        } else cell
                    }
                },
            ),
        )
    }

    companion object {
        const val CELLS_PER_PAGE = 24

        /** Move an entry from [from] to slot [to] (page -1 = dock). */
        fun moveSlot(
            layout: com.linehu.asi.model.HomeLayout,
            from: Slot,
            to: Slot,
        ): com.linehu.asi.model.HomeLayout {
            if (from == to) return layout
            val dock = layout.dock.toMutableList()
            val pages = layout.pages.map { it.toMutableList() }.toMutableList()

            fun listAt(page: Int): MutableList<GridEntry>? = when (page) {
                -1 -> dock
                else -> pages.getOrNull(page)
            }

            val src = listAt(from.page) ?: return layout
            val entry = src.getOrNull(from.index) ?: return layout
            src.removeAt(from.index)
            val dst = listAt(to.page) ?: return layout
            val idx = to.index.coerceIn(0, dst.size)
            dst.add(idx, entry)
            // An over-full page pushes its tail onto the next page (created
            // if it does not exist yet), like iOS.
            if (to.page >= 0 && dst.size > CELLS_PER_PAGE) {
                val spilled = dst.removeAt(dst.lastIndex)
                if (to.page + 1 < pages.size) {
                    pages[to.page + 1].add(0, spilled)
                } else {
                    pages.add(to.page + 1, mutableListOf(spilled))
                }
            }
            return com.linehu.asi.model.HomeLayout(dock = dock, pages = pages)
        }

        /** Drop [from] onto an occupied [onto] slot → merge into a folder. */
        fun mergeSlots(
            layout: com.linehu.asi.model.HomeLayout,
            from: Slot,
            onto: Slot,
        ): com.linehu.asi.model.HomeLayout {
            val dock = layout.dock.toMutableList()
            val pages = layout.pages.map { it.toMutableList() }.toMutableList()

            fun listAt(page: Int): MutableList<GridEntry>? = when (page) {
                -1 -> dock
                else -> pages.getOrNull(page)
            }

            val src = listAt(from.page) ?: return layout
            val dst = listAt(onto.page) ?: return layout
            val moving = src.getOrNull(from.index) ?: return layout
            val target = dst.getOrNull(onto.index) ?: return layout
            if (moving == target) return layout

            src.removeAt(from.index)
            if (onto.page == -1) {
                // No folders in the dock (iOS behavior) — plain move instead.
                dst.add(onto.index.coerceIn(0, dst.size), moving)
            } else {
                val folder = when (target) {
                    is GridEntry.FolderEntry -> target.copy(children = target.children + moving)
                    else -> GridEntry.FolderEntry(
                        id = java.util.UUID.randomUUID().toString(),
                        title = "新建文件夹",
                        children = listOf(target, moving),
                    )
                }
                dst[onto.index] = folder
            }
            return com.linehu.asi.model.HomeLayout(dock = dock, pages = pages)
        }

        fun removeSlot(
            layout: com.linehu.asi.model.HomeLayout,
            page: Int,
            index: Int,
        ): com.linehu.asi.model.HomeLayout {
            return if (page == -1) {
                val dock = layout.dock.toMutableList()
                if (index >= dock.size) return layout
                dock.removeAt(index)
                layout.copy(dock = dock)
            } else {
                val pages = layout.pages.map { it.toMutableList() }
                if (page >= pages.size || index >= pages[page].size) return layout
                pages[page].removeAt(index)
                layout.copy(pages = pages)
            }
        }

        fun removeFolderChild(
            layout: com.linehu.asi.model.HomeLayout,
            folderId: String,
            childIndex: Int,
        ): com.linehu.asi.model.HomeLayout {
            val pages = layout.pages.map { page ->
                page.mapNotNull { cell ->
                    if (cell is GridEntry.FolderEntry && cell.id == folderId) {
                        val children = cell.children.toMutableList()
                        if (childIndex >= children.size) return@mapNotNull cell
                        children.removeAt(childIndex)
                        when {
                            children.isEmpty() -> null // empty folder disappears
                            children.size == 1 -> children.first() // auto-unfolder, like iOS
                            else -> cell.copy(children = children)
                        }
                    } else cell
                }
            }
            return layout.copy(pages = pages)
        }
    }

    /** Uninstall an external app (system confirmation sheet). */
    fun uninstall(appKey: String) {
        val app = appsState.value[appKey] ?: return
        val intent = android.content.Intent(
            android.content.Intent.ACTION_DELETE,
            android.net.Uri.parse("package:${app.componentName.packageName}"),
        )
        intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { appContext.startActivity(intent) }
    }

    fun labelOf(entry: GridEntry): String = when (entry) {
        is GridEntry.AppEntry -> appsState.value[entry.key]?.label ?: ""
        is GridEntry.BuiltinEntry -> entry.appId.displayLabel
        is GridEntry.FolderEntry -> entry.title
    }

    /** Reconcile persisted layout with the current package set (add/remove). */
    private suspend fun syncLayoutWithInstalledApps() {
        val layout = layoutState.value
        if (layout == HomeLayout.EMPTY) return
        val referenced = LayoutRepository.allAppKeys(layout)
        val installed = appsState.value.keys
        val stale = referenced - installed
        val missing = installed - referenced
        if (stale.isEmpty() && missing.isEmpty()) return
        var next = layout
        stale.forEach { key -> next = LayoutRepository.withoutApp(next, key) }
        missing.sorted().forEach { key -> next = LayoutRepository.withAppAppended(next, key) }
        layoutRepository.save(next)
    }
}
