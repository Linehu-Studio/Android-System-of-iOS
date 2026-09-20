package com.linehu.asi.data.layout

import com.linehu.asi.model.GridEntry
import com.linehu.asi.model.HomeLayout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Owns the persisted home screen arrangement. Exposes the layout as a flow
 * and persists whole-layout rewrites (the dataset is tiny — a full rewrite
 * keeps drag/drop persistence trivial and consistent).
 */
class LayoutRepository(
    private val dao: LayoutDao,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
) {

    val layout: Flow<HomeLayout> = combine(
        dao.homeItemsFlow(),
        dao.foldersFlow(),
        dao.folderItemsFlow(),
    ) { items, folders, folderItems ->
        LayoutMapper.toLayout(items, folders, folderItems)
    }

    suspend fun isEmpty(): Boolean = dao.homeItemCount() == 0

    suspend fun save(layout: HomeLayout) = withContext(Dispatchers.IO) {
        val (items, folders, folderItems) = LayoutMapper.fromLayout(layout)
        dao.replaceLayout(items, folders, folderItems)
    }

    /** Ensures a first-run layout exists; no-op when the DB already has rows. */
    suspend fun ensureInitialized(defaults: HomeLayout) {
        if (isEmpty()) save(defaults)
    }

    fun saveAsync(layout: HomeLayout) {
        scope.launch { save(layout) }
    }

    /** Adds an app to the first free slot on the last page (new installs). */
    suspend fun appendApp(key: String) {
        save(withAppAppended(layout.first(), key))
    }

    /** Removes an app from wherever it lives (dock, grid or folder). */
    suspend fun removeApp(key: String) {
        save(withoutApp(layout.first(), key))
    }

    companion object {
        const val CELLS_PER_PAGE = 24

        /** Pure: [layout] with [key] appended to the last page (new page if full). */
        fun withAppAppended(layout: HomeLayout, key: String): HomeLayout {
            val entry = GridEntry.AppEntry(key)
            if (containsApp(layout, key)) return layout
            val pages = layout.pages.toMutableList()
            val last = pages.removeLastOrNull() ?: emptyList()
            if (last.size < CELLS_PER_PAGE) {
                pages.add(last + entry)
            } else {
                pages.add(last)
                pages.add(listOf(entry))
            }
            return layout.copy(pages = pages)
        }

        /** Pure: [layout] with every occurrence of [key] removed. */
        fun withoutApp(layout: HomeLayout, key: String): HomeLayout {
            val entry = GridEntry.AppEntry(key)
            return HomeLayout(
                dock = layout.dock.filterNot { it == entry },
                pages = layout.pages.map { page ->
                    page.mapNotNull { cell ->
                        when {
                            cell == entry -> null
                            cell is GridEntry.FolderEntry ->
                                cell.copy(children = cell.children.filterNot { it == entry })
                                .takeIf { it.children.isNotEmpty() }
                            else -> cell
                        }
                    }
                },
            )
        }

        fun containsApp(layout: HomeLayout, key: String): Boolean {
            val entry = GridEntry.AppEntry(key)
            if (layout.dock.any { it == entry }) return true
            return layout.pages.any { page ->
                page.any { cell -> cell == entry || (cell is GridEntry.FolderEntry && cell.children.any { it == entry }) }
            }
        }

        /** All external app keys referenced anywhere in the layout. */
        fun allAppKeys(layout: HomeLayout): Set<String> {
            val keys = mutableSetOf<String>()
            fun scan(entries: List<GridEntry>) {
                entries.forEach { cell ->
                    when (cell) {
                        is GridEntry.AppEntry -> keys += cell.key
                        is GridEntry.FolderEntry -> scan(cell.children)
                        is GridEntry.BuiltinEntry -> Unit
                    }
                }
            }
            scan(layout.dock)
            layout.pages.forEach(::scan)
            return keys
        }
    }
}
