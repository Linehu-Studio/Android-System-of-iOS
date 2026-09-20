package com.linehu.asi.feature.springboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.linehu.asi.core.designsystem.FrostedPanel
import com.linehu.asi.model.GridEntry
import kotlinx.coroutines.launch

/**
 * The iOS home screen: paged icon grid, page dots, frosted dock, folder
 * overlay, spotlight pull-down and jiggle-mode editing.
 */
@Composable
fun SpringboardScreen(
    pages: List<List<GridEntry>>,
    dock: List<GridEntry>,
    apps: List<com.linehu.asi.model.AppInfo>,
    iconLoader: (suspend (String, Int) -> ImageBitmap?)?,
    labelOf: (GridEntry) -> String,
    onOpen: (GridEntry) -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier,
    jiggle: Boolean = false,
    onJiggleDone: () -> Unit = {},
    onMove: (page: Int, from: Int, to: Int) -> Unit = { _, _, _ -> },
    onMerge: (page: Int, from: Int, onto: Int) -> Unit = { _, _, _ -> },
    onRemove: (page: Int, index: Int) -> Unit = { _, _ -> },
    onRenameFolder: (folderId: String, title: String) -> Unit = { _, _ -> },
) {
    val pagerState = rememberPagerState(pageCount = { pages.size.coerceAtLeast(1) })
    val scope = rememberCoroutineScope()
    var openFolder by remember { mutableStateOf<GridEntry.FolderEntry?>(null) }
    var spotlightOpen by remember { mutableStateOf(false) }

    Box(modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f),
            ) { page ->
                HomePageGrid(
                    entries = pages.getOrNull(page).orEmpty(),
                    iconLoader = iconLoader,
                    labelOf = labelOf,
                    onOpen = onOpen,
                    onLongPress = onLongPress,
                    jiggle = jiggle,
                    onMove = { from, to -> onMove(page, from, to) },
                    onMerge = { from, onto -> onMerge(page, from, onto) },
                    onRemove = { index -> onRemove(page, index) },
                )
            }
            // Page dots
            Row(
                Modifier.fillMaxWidth().height(20.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                repeat(pages.size.coerceAtLeast(1)) { i ->
                    val active = i == pagerState.currentPage
                    Box(
                        Modifier
                            .padding(horizontal = 3.dp)
                            .size(if (active) 7.dp else 6.dp)
                            .clip(CircleShape)
                            .background(if (active) Color.White else Color(0x66FFFFFF)),
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Dock(
                entries = dock,
                iconLoader = iconLoader,
                labelOf = labelOf,
                onOpen = onOpen,
                onLongPress = onLongPress,
                jiggle = jiggle,
            )
            Spacer(Modifier.navigationBarsPadding().height(8.dp))
        }

        // Spotlight pull-down detection on empty background (below the grid
        // interactions). Attach to the whole surface but only react to
        // clearly vertical downward drags.
        Box(
            Modifier
                .fillMaxSize()
                .pointerInput(jiggle) {
                    if (jiggle) return@pointerInput
                    detectVerticalDragGestures { change, dragAmount ->
                        change.consume()
                        if (dragAmount > 55f) spotlightOpen = true
                    }
                },
        )

        // "Done" pill in jiggle mode
        if (jiggle) {
            Box(
                Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 54.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xCC48484A))
                    .clickable { onJiggleDone() }
                    .padding(horizontal = 22.dp, vertical = 8.dp),
            ) {
                com.linehu.asi.core.designsystem.IosText("完成", color = Color.White, fontSize = 15.sp)
            }
        }

        // Folder overlay
        openFolder?.let { folder ->
            FolderOverlay(
                folder = folder,
                iconLoader = iconLoader,
                labelOf = labelOf,
                onOpen = { child ->
                    openFolder = null
                    onOpen(child)
                },
                onRename = { title -> onRenameFolder(folder.id, title) },
                onDismiss = { openFolder = null },
            )
        }

        // Spotlight overlay
        if (spotlightOpen) {
            SpotlightOverlay(
                apps = apps,
                iconLoader = iconLoader,
                onOpen = onOpen,
                onDismiss = { spotlightOpen = false },
            )
        }
    }
}

@Composable
private fun Dock(
    entries: List<GridEntry>,
    iconLoader: (suspend (String, Int) -> ImageBitmap?)?,
    labelOf: (GridEntry) -> String,
    onOpen: (GridEntry) -> Unit,
    onLongPress: () -> Unit,
    jiggle: Boolean,
) {
    FrostedPanel(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .height(96.dp)
            .clip(RoundedCornerShape(32.dp)),
    ) {
        Row(
            Modifier.fillMaxSize().padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            entries.take(4).forEach { entry ->
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    IconCell(
                        entry = entry,
                        label = "",
                        iconLoader = iconLoader,
                        onClick = { if (!jiggle) onOpen(entry) },
                        onLongClick = onLongPress,
                        jiggle = jiggle,
                        modifier = Modifier.size(60.dp),
                    )
                }
            }
        }
    }
}

/** Fullscreen folder: frosted background, editable title, child grid. */
@Composable
private fun FolderOverlay(
    folder: GridEntry.FolderEntry,
    iconLoader: (suspend (String, Int) -> ImageBitmap?)?,
    labelOf: (GridEntry) -> String,
    onOpen: (GridEntry) -> Unit,
    onRename: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var title by remember(folder.id) { mutableStateOf(folder.title) }
    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0x99000000))
            .pointerInput(Unit) {
                detectVerticalDragGestures { change, dragAmount ->
                    change.consume()
                }
            },
    ) {
        // Tap outside the folder card closes it.
        Box(
            Modifier
                .fillMaxSize()
                .clickable(onClick = onDismiss),
        )
        Column(
            Modifier
                .align(Alignment.Center)
                .padding(horizontal = 28.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(40.dp))
                .background(Color(0xD9C7C7CC))
                .padding(vertical = 26.dp, horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            BasicTextField(
                value = title,
                onValueChange = {
                    title = it
                    onRename(it)
                },
                textStyle = androidx.compose.ui.text.TextStyle(
                    color = Color(0xFF0A84FF),
                    fontSize = 20.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                ),
                cursorBrush = SolidColor(Color(0xFF0A84FF)),
                modifier = Modifier.padding(bottom = 18.dp),
            )
            Column {
                folder.children.chunked(4).forEach { rowEntries ->
                    Row {
                        rowEntries.forEach { child ->
                            Box(
                                Modifier
                                    .weight(1f)
                                    .padding(8.dp),
                                contentAlignment = Alignment.TopCenter,
                            ) {
                                IconCell(
                                    entry = child,
                                    label = labelOf(child),
                                    iconLoader = iconLoader,
                                    onClick = { onOpen(child) },
                                )
                            }
                        }
                        // Fill remaining cells so weights align.
                        repeat(4 - rowEntries.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }
    }
}
