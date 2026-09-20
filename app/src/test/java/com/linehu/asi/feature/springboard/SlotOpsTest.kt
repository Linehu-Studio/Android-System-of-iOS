package com.linehu.asi.feature.springboard

import com.linehu.asi.model.AppId
import com.linehu.asi.model.GridEntry
import com.linehu.asi.model.HomeLayout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SlotOpsTest {

    private fun app(key: String) = GridEntry.AppEntry(key)

    private fun layoutOf(pages: List<List<GridEntry>>, dock: List<GridEntry> = emptyList()) =
        HomeLayout(dock = dock, pages = pages)

    @Test
    fun `move within page reorders`() {
        val layout = layoutOf(listOf(listOf(app("a"), app("b"), app("c"))))
        val moved = SpringboardViewModel.moveSlot(layout, SpringboardViewModel.Slot(0, 0), SpringboardViewModel.Slot(0, 2))
        assertEquals(listOf("b", "c", "a"), moved.pages[0].map { (it as GridEntry.AppEntry).key })
    }

    @Test
    fun `move to dock`() {
        val layout = layoutOf(listOf(listOf(app("a"), app("b"))))
        val moved = SpringboardViewModel.moveSlot(layout, SpringboardViewModel.Slot(0, 1), SpringboardViewModel.Slot(-1, 0))
        assertEquals("b", (moved.dock[0] as GridEntry.AppEntry).key)
        assertEquals(1, moved.pages[0].size)
    }

    @Test
    fun `move across pages`() {
        val layout = layoutOf(listOf(listOf(app("a")), listOf(app("b"), app("c"))))
        val moved = SpringboardViewModel.moveSlot(layout, SpringboardViewModel.Slot(1, 0), SpringboardViewModel.Slot(0, 1))
        assertEquals(listOf("a", "b"), moved.pages[0].map { (it as GridEntry.AppEntry).key })
        assertEquals(listOf("c"), moved.pages[1].map { (it as GridEntry.AppEntry).key })
    }

    @Test
    fun `overflow page spills to the next page`() {
        val layout = layoutOf(
            listOf(
                List(SpringboardViewModel.CELLS_PER_PAGE) { app("p$it") },
                listOf(app("z")),
            ),
        )
        val moved = SpringboardViewModel.moveSlot(
            layout,
            SpringboardViewModel.Slot(1, 0), // "z" moves into the full page 0
            SpringboardViewModel.Slot(0, 0),
        )
        // Page 0 stays full (now starting with z), its former tail "p23"
        // spills onto page 1.
        assertEquals(SpringboardViewModel.CELLS_PER_PAGE, moved.pages[0].size)
        assertEquals("z", (moved.pages[0][0] as GridEntry.AppEntry).key)
        assertEquals(1, moved.pages[1].size)
        assertEquals("p23", (moved.pages[1][0] as GridEntry.AppEntry).key)
    }

    @Test
    fun `merge two apps creates a folder`() {
        val layout = layoutOf(listOf(listOf(app("a"), app("b"), app("c"))))
        val merged = SpringboardViewModel.mergeSlots(
            layout,
            SpringboardViewModel.Slot(0, 2),
            SpringboardViewModel.Slot(0, 0),
        )
        val folder = merged.pages[0][0] as GridEntry.FolderEntry
        assertEquals(2, folder.children.size)
        // iOS behavior: the folder occupies the target slot, the rest shifts.
        assertEquals(2, merged.pages[0].size)
        assertEquals("b", (merged.pages[0][1] as GridEntry.AppEntry).key)
    }

    @Test
    fun `merge onto a folder appends`() {
        val folder = GridEntry.FolderEntry("f1", "工具", listOf(app("a")))
        val layout = layoutOf(listOf(listOf(folder, app("b"))))
        val merged = SpringboardViewModel.mergeSlots(
            layout,
            SpringboardViewModel.Slot(0, 1),
            SpringboardViewModel.Slot(0, 0),
        )
        val mergedFolder = merged.pages[0][0] as GridEntry.FolderEntry
        assertEquals(2, mergedFolder.children.size)
    }

    @Test
    fun `removing last-but-one child unfolds the folder`() {
        val folder = GridEntry.FolderEntry("f1", "工具", listOf(app("a"), app("b")))
        val layout = layoutOf(listOf(listOf(folder)))
        val result = SpringboardViewModel.removeFolderChild(layout, "f1", 0)
        // 1 child left → auto-unfold into a plain app cell.
        assertTrue(result.pages[0][0] is GridEntry.AppEntry)
    }

    @Test
    fun `removing every child removes the folder`() {
        val folder = GridEntry.FolderEntry("f1", "工具", listOf(app("a")))
        val layout = layoutOf(listOf(listOf(folder, app("b"))))
        val result = SpringboardViewModel.removeFolderChild(layout, "f1", 0)
        assertEquals(listOf("b"), result.pages[0].map { (it as GridEntry.AppEntry).key })
    }

    @Test
    fun `remove slot shifts the page`() {
        val layout = layoutOf(listOf(listOf(app("a"), app("b"), app("c"))))
        val result = SpringboardViewModel.removeSlot(layout, 0, 1)
        assertEquals(listOf("a", "c"), result.pages[0].map { (it as GridEntry.AppEntry).key })
    }

    @Test
    fun `move to same slot is a no-op`() {
        val layout = layoutOf(listOf(listOf(app("a"), app("b"))))
        assertEquals(layout, SpringboardViewModel.moveSlot(layout, SpringboardViewModel.Slot(0, 0), SpringboardViewModel.Slot(0, 0)))
    }
}
