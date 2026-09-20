package com.linehu.asi.feature.springboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.linehu.asi.core.designsystem.FrostedPanel
import com.linehu.asi.core.designsystem.IosText
import com.linehu.asi.model.AppId
import com.linehu.asi.model.AppInfo
import com.linehu.asi.model.GridEntry

/**
 * Spotlight search: fuzzy-ish contains match over installed apps and
 * built-ins. (Pinyin matching is a known MVP gap.)
 */
@Composable
fun SpotlightOverlay(
    apps: List<AppInfo>,
    iconLoader: (suspend (String, Int) -> ImageBitmap?)?,
    onOpen: (GridEntry) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var query by remember { mutableStateOf("") }
    val builtins = AppId.entries.map { GridEntry.BuiltinEntry(it) }

    val results = remember(query, apps) {
        if (query.isBlank()) {
            emptyList()
        } else {
            val q = query.trim()
            val appMatches = apps
                .filter { it.label.contains(q, ignoreCase = true) }
                .take(12)
                .map { GridEntry.AppEntry(it.key) to it.label }
            val builtinMatches = builtins
                .filter { it.appId.displayLabel.contains(q, ignoreCase = true) }
                .map { it to it.appId.displayLabel }
            appMatches + builtinMatches
        }
    }

    Box(modifier.fillMaxSize()) {
        // Tap-away scrim
        Box(Modifier.fillMaxSize().background(Color(0x66000000)).clickable(onClick = onDismiss))
        FrostedPanel(
            Modifier
                .fillMaxWidth()
                .padding(top = 72.dp, start = 20.dp, end = 20.dp)
                .height(520.dp)
                .clip(RoundedCornerShape(24.dp)),
        ) {
            Column(Modifier.fillMaxSize().padding(16.dp)) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0x1F3C3C43))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Filled.Search, null, tint = Color(0x993C3C43), modifier = Modifier.size(20.dp))
                    Spacer(Modifier.size(8.dp))
                    androidx.compose.material3.TextField(
                        value = query,
                        onValueChange = { query = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { IosText("搜索应用", color = Color(0x663C3C43), fontSize = 15.sp) },
                        singleLine = true,
                        colors = androidx.compose.material3.TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedTextColor = Color(0xFF0A84FF),
                            unfocusedTextColor = Color(0xFF0A84FF),
                            cursorColor = Color(0xFF0A84FF),
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                        ),
                    )
                }
                Spacer(Modifier.height(10.dp))
                if (results.isEmpty()) {
                    if (query.isBlank()) {
                        IosText(
                            "下拉或点击开始搜索",
                            color = Color(0x663C3C43),
                            fontSize = 14.sp,
                            modifier = Modifier.padding(top = 20.dp).align(Alignment.CenterHorizontally),
                        )
                    } else {
                        IosText(
                            "没有匹配的应用",
                            color = Color(0x663C3C43),
                            fontSize = 14.sp,
                            modifier = Modifier.padding(top = 20.dp).align(Alignment.CenterHorizontally),
                        )
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        items(results, key = { it.first.stableId }) { (entry, label) ->
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        onOpen(entry)
                                        onDismiss()
                                    }
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(
                                    Modifier.size(34.dp).clip(CircleShape).background(Color(0x1F3C3C43)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    if (entry is GridEntry.BuiltinEntry) {
                                        BuiltinAppIcon(entry.appId, Modifier.size(28.dp))
                                    } else {
                                        IosText(
                                            label.take(1),
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFF0A84FF),
                                        )
                                    }
                                }
                                Spacer(Modifier.size(12.dp))
                                IosText(label, fontSize = 16.sp, color = Color(0xFF0A84FF))
                            }
                        }
                    }
                }
            }
        }
    }
}
