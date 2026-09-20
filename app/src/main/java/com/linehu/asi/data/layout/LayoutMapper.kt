package com.linehu.asi.data.layout

import com.linehu.asi.model.AppId
import com.linehu.asi.model.GridEntry
import com.linehu.asi.model.HomeLayout

/**
 * Pure mapping between Room rows and the [HomeLayout] UI model. Kept side
 * effect free so it is unit-testable.
 */
object LayoutMapper {

    fun toLayout(
        items: List<HomeItemEntity>,
        folders: List<FolderEntity>,
        folderItems: List<FolderItemEntity>,
    ): HomeLayout {
        val childrenByFolder = folderItems
            .groupBy { it.folderId }
            .mapValues { (_, rows) -> rows.sortedBy { it.slot }.mapNotNull(::folderChildToEntry) }

        fun folderToEntry(f: FolderEntity) = GridEntry.FolderEntry(
            f.folderId,
            f.title,
            childrenByFolder[f.folderId].orEmpty(),
        )

        val dock = items
            .filter { it.page == HomeItemEntity.PAGE_DOCK }
            .sortedBy { it.slot }
            .mapNotNull(::itemToEntry)

        val foldersByPage = folders.groupBy { it.page }
            .mapValues { (_, fs) -> fs.associateBy { it.slot }.toMutableMap() }

        val pages: MutableList<List<GridEntry>> = items
            .filter { it.page >= 0 }
            .groupBy { it.page }
            .toSortedMap()
            .map { (page, pageItems) ->
                val folderMap = foldersByPage[page] ?: mutableMapOf()
                val entries = mutableListOf<GridEntry>()
                var slot = 0
                for (entity in pageItems.sortedBy { it.slot }) {
                    // Folders win slot collisions: they are emitted at their
                    // recorded slot, items shift past them.
                    while (folderMap.containsKey(slot)) {
                        entries += folderToEntry(folderMap.remove(slot)!!)
                        slot++
                    }
                    itemToEntry(entity)?.let { entries += it; slot++ }
                }
                // Any folder rows still unplaced (dangling indices) go last.
                folderMap.values.sortedBy { it.slot }.forEach { entries += folderToEntry(it) }
                entries
            }
            .toMutableList()

        // Folders on a page that has no plain items at all.
        foldersByPage.keys
            .filter { it >= 0 && it !in items.map { i -> i.page }.toSet() }
            .sorted()
            .forEach { page ->
                val entries: List<GridEntry> = foldersByPage[page]?.values?.sortedBy { it.slot }
                    ?.map { folderToEntry(it) }.orEmpty()
                pages.add(page.coerceAtMost(pages.size), entries)
            }

        if (pages.isEmpty()) pages.add(emptyList())
        return HomeLayout(dock = dock, pages = pages)
    }

    private fun itemToEntry(entity: HomeItemEntity): GridEntry? = when (entity.kind) {
        HomeItemEntity.KIND_APP -> entity.appKey?.let { GridEntry.AppEntry(it) }
        HomeItemEntity.KIND_BUILTIN -> entity.appId?.let { GridEntry.BuiltinEntry(AppId.valueOf(it)) }
        else -> null
    }

    private fun folderChildToEntry(entity: FolderItemEntity): GridEntry? = when (entity.kind) {
        HomeItemEntity.KIND_APP -> entity.appKey?.let { GridEntry.AppEntry(it) }
        HomeItemEntity.KIND_BUILTIN -> entity.appId?.let { GridEntry.BuiltinEntry(AppId.valueOf(it)) }
        else -> null
    }

    fun fromLayout(layout: HomeLayout): Triple<List<HomeItemEntity>, List<FolderEntity>, List<FolderItemEntity>> {
        val items = mutableListOf<HomeItemEntity>()
        val folders = mutableListOf<FolderEntity>()
        val folderItems = mutableListOf<FolderItemEntity>()

        fun addCell(entry: GridEntry, page: Int, slot: Int) {
            when (entry) {
                is GridEntry.AppEntry ->
                    items += HomeItemEntity(kind = HomeItemEntity.KIND_APP, appKey = entry.key, page = page, slot = slot)
                is GridEntry.BuiltinEntry ->
                    items += HomeItemEntity(kind = HomeItemEntity.KIND_BUILTIN, appId = entry.appId.name, page = page, slot = slot)
                is GridEntry.FolderEntry -> {
                    folders += FolderEntity(entry.id, entry.title, page, slot)
                    entry.children.forEachIndexed { childSlot, child ->
                        when (child) {
                            is GridEntry.AppEntry ->
                                folderItems += FolderItemEntity(folderId = entry.id, slot = childSlot, kind = HomeItemEntity.KIND_APP, appKey = child.key)
                            is GridEntry.BuiltinEntry ->
                                folderItems += FolderItemEntity(folderId = entry.id, slot = childSlot, kind = HomeItemEntity.KIND_BUILTIN, appId = child.appId.name)
                            is GridEntry.FolderEntry -> Unit // nested folders not supported (as on iOS)
                        }
                    }
                }
            }
        }

        layout.dock.forEachIndexed { slot, entry -> addCell(entry, HomeItemEntity.PAGE_DOCK, slot) }
        layout.pages.forEachIndexed { page, cells ->
            cells.forEachIndexed { slot, entry -> addCell(entry, page, slot) }
        }
        return Triple(items, folders, folderItems)
    }
}
