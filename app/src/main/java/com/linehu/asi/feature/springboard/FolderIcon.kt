package com.linehu.asi.feature.springboard

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Color
import com.linehu.asi.core.designsystem.LocalIosColors
import com.linehu.asi.core.designsystem.SquircleShape
import com.linehu.asi.core.designsystem.folderShape
import com.linehu.asi.model.GridEntry

/**
 * iOS folder icon: frosted squircle with a 2×2 mini preview of the first
 * four children.
 */
@Composable
fun FolderIcon(entry: GridEntry.FolderEntry, modifier: Modifier = Modifier) {
    val colors = LocalIosColors.current
    Box(
        modifier
            .clip(SquircleShape())
            .background(colors.surfaceBlurTint),
        contentAlignment = Alignment.Center,
    ) {
        Column(Modifier.fillMaxSize().padding(6.dp)) {
            val preview = entry.children.take(4)
            repeat(2) { r ->
                Row(Modifier.weight(1f).fillMaxWidth()) {
                    repeat(2) { c ->
                        val index = r * 2 + c
                        val child = preview.getOrNull(index)
                        Box(
                            Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .padding(3.dp)
                                .clip(SquircleShape())
                                .background(Color.Transparent),
                            contentAlignment = Alignment.Center,
                        ) {
                            child?.let {
                                MiniIcon(it, Modifier.fillMaxSize())
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MiniIcon(entry: GridEntry, modifier: Modifier = Modifier) {
    when (entry) {
        is GridEntry.BuiltinEntry -> BuiltinAppIcon(entry.appId, modifier)
        is GridEntry.AppEntry ->
            // External app icons inside a folder preview: neutral tinted tile.
            Box(modifier.clip(SquircleShape()).background(Color(0x4DA8A8AE)))
        is GridEntry.FolderEntry -> Box(modifier)
    }
}
