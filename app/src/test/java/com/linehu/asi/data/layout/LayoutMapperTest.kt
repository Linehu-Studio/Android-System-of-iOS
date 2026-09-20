package com.linehu.asi.data.layout

import com.linehu.asi.model.AppId
import com.linehu.asi.model.GridEntry
import com.linehu.asi.model.HomeLayout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LayoutMapperTest {

    private fun app(key: String) = GridEntry.AppEntry(key)
    private fun builtin(id: AppId) = GridEntry.BuiltinEntry(id)

    @Test
    fun `round trip preserves dock and pages`() {
        val layout = HomeLayout(
            dock = listOf(builtin(AppId.SETTINGS), app("com.a/.Main")),
            pages = listOf(
                listOf(builtin(AppId.CLOCK), builtin(AppId.NOTES), app("com.b/.Main")),
                listOf(app("com.c/.Main")),
            ),
        )
        val (items, folders, folderItems) = LayoutMapper.fromLayout(layout)
        val restored = LayoutMapper.toLayout(items, folders, folderItems)
        assertEquals(layout, restored)
    }

    @Test
    fun `round trip preserves folders`() {
        val folder = GridEntry.FolderEntry(
            id = "f1",
            title = "工具",
            children = listOf(app("com.a/.Main"), builtin(AppId.CALCULATOR)),
        )
        val layout = HomeLayout(dock = emptyList(), pages = listOf(listOf(folder, app("com.z/.Main"))))
        val (items, folders, folderItems) = LayoutMapper.fromLayout(layout)
        val restored = LayoutMapper.toLayout(items, folders, folderItems)
        assertEquals(layout, restored)
    }

    @Test
    fun `empty layout maps to single empty page`() {
        val restored = LayoutMapper.toLayout(emptyList(), emptyList(), emptyList())
        assertEquals(HomeLayout.EMPTY, restored)
    }

    @Test
    fun `appended and removed apps survive persistence round trip`() {
        var layout = HomeLayout(
            dock = emptyList(),
            pages = listOf(List(24) { app("com.app$it/.Main") }),
        )
        // Persist and restore first so the mapper path is covered.
        val (i1, f1, fi1) = LayoutMapper.fromLayout(layout)
        layout = LayoutMapper.toLayout(i1, f1, fi1)
        assertEquals(1, layout.pages.size)

        // Append onto a full page → new page.
        layout = com.linehu.asi.data.layout.LayoutRepository.withAppAppended(layout, "com.new/.Main")
        assertEquals(2, layout.pages.size)
        assertEquals("com.new/.Main", (layout.pages[1][0] as GridEntry.AppEntry).key)

        // Remove an app that lives mid-page.
        layout = com.linehu.asi.data.layout.LayoutRepository.withoutApp(layout, "com.app10/.Main")
        assertTrue(
            layout.pages.all { page ->
                page.none { it == GridEntry.AppEntry("com.app10/.Main") }
            },
        )
    }
}
