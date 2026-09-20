package com.linehu.asi.feature.springboard

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.linehu.asi.model.GridEntry
import kotlin.math.abs

object GridSpec {
    const val COLUMNS = 4
    const val ROWS = 6
    const val CELLS_PER_PAGE = COLUMNS * ROWS
}

/**
 * Fixed (non-scrolling) icon page. In jiggle mode icons can be dragged:
 * drop on an empty cell reorders, drop on an occupied cell merges into a
 * folder (or appends to an existing one).
 */
@Composable
fun HomePageGrid(
    entries: List<GridEntry>,
    iconLoader: (suspend (String, Int) -> ImageBitmap?)?,
    labelOf: (GridEntry) -> String,
    onOpen: (GridEntry) -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier,
    jiggle: Boolean = false,
    onMove: (from: Int, to: Int) -> Unit = { _, _ -> },
    onMerge: (from: Int, onto: Int) -> Unit = { _, _ -> },
    onRemove: (index: Int) -> Unit = {},
) {
    val cellCenters = remember { mutableStateMapOf<Int, Offset>() }
    var dragIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }

    fun nearestCell(to: Offset): Int? =
        cellCenters.entries.minByOrNull { abs(it.value.x - to.x) + abs(it.value.y - to.y) }?.key

    Column(modifier = modifier.fillMaxSize().padding(horizontal = 18.dp)) {
        repeat(GridSpec.ROWS) { row ->
            Row(Modifier.weight(1f).fillMaxWidth()) {
                repeat(GridSpec.COLUMNS) { col ->
                    val index = row * GridSpec.COLUMNS + col
                    val entry = entries.getOrNull(index)
                    Box(
                        Modifier
                            .weight(1f)
                            .aspectRatio(0.82f)
                            .padding(4.dp)
                            .onGloballyPositioned { coords ->
                                cellCenters[index] = coords.positionInRoot() +
                                    Offset(coords.size.width / 2f, coords.size.height / 2f)
                            }
                            .pointerInput(jiggle, entries) {
                                if (!jiggle) return@pointerInput
                                detectDragGestures(
                                    onDragStart = {
                                        if (entry != null) {
                                            dragIndex = index
                                            dragOffset = Offset.Zero
                                        }
                                    },
                                    onDrag = { change, amount ->
                                        change.consume()
                                        dragOffset += amount
                                    },
                                    onDragEnd = {
                                        val from = dragIndex
                                        val start = cellCenters[from]
                                        if (from != null && start != null) {
                                            val finger = start + dragOffset
                                            val target = nearestCell(finger)
                                            when {
                                                target == null || target == from -> Unit
                                                entries.getOrNull(target) == null -> onMove(from, target)
                                                else -> onMerge(from, target)
                                            }
                                        }
                                        dragIndex = null
                                        dragOffset = Offset.Zero
                                    },
                                    onDragCancel = {
                                        dragIndex = null
                                        dragOffset = Offset.Zero
                                    },
                                )
                            },
                        contentAlignment = Alignment.TopCenter,
                    ) {
                        if (entry != null) {
                            IconCell(
                                entry = entry,
                                label = labelOf(entry),
                                iconLoader = iconLoader,
                                onClick = { if (!jiggle) onOpen(entry) },
                                onLongClick = onLongPress,
                                jiggle = jiggle,
                                showMinus = jiggle && entry !is GridEntry.FolderEntry,
                                onMinus = { onRemove(index) },
                                dragging = dragIndex == index,
                                dragOffsetX = if (dragIndex == index) dragOffset.x else 0f,
                                dragOffsetY = if (dragIndex == index) dragOffset.y else 0f,
                                modifier = Modifier.zIndex(if (dragIndex == index) 10f else 0f),
                            )
                        }
                    }
                }
            }
        }
    }
}
