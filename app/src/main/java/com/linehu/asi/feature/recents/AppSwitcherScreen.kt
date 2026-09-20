package com.linehu.asi.feature.recents

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.linehu.asi.core.designsystem.IosStatusBar
import com.linehu.asi.core.designsystem.IosText
import com.linehu.asi.data.apps.IconCache
import com.linehu.asi.data.recents.RecentsRepository
import com.linehu.asi.model.AppInfo
import com.linehu.asi.model.RecentsEntry
import com.linehu.asi.util.asiContainer
import kotlinx.coroutines.flow.first

/**
 * iOS-style app switcher: swipeable cards. Tap reopens the app; swiping a
 * card up removes it from our history (it does not kill the process).
 */
@Composable
fun AppSwitcherScreen(
    repository: RecentsRepository,
    iconCache: IconCache,
    usageStatsEnabled: Boolean,
    onOpenApp: (AppInfo) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val container = context.asiContainer()
    var recents by remember(usageStatsEnabled) { mutableStateOf<List<RecentsEntry>>(emptyList()) }

    LaunchedEffect(usageStatsEnabled) {
        recents = runCatching { repository.recents(includeUsageStats = usageStatsEnabled) }
            .getOrDefault(emptyList())
    }
    var appMap by remember { mutableStateOf<Map<String, AppInfo>>(emptyMap()) }
    LaunchedEffect(Unit) { appMap = container.appRepository.appsByKey.first() }

    Box(modifier.fillMaxSize().background(Color(0xE6101014))) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            IosStatusBar(contentColor = Color.White)
            if (recents.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.Android, null, tint = Color(0x33FFFFFF), modifier = Modifier.size(64.dp))
                        Spacer(Modifier.height(10.dp))
                        IosText("最近没有从 ASI 启动的应用", color = Color(0x66EBEBF5), fontSize = 15.sp)
                    }
                }
            } else {
                LazyRow(
                    Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(horizontal = 28.dp),
                ) {
                    items(recents, key = { it.appKey }) { entry ->
                        RecentsCard(
                            entry = entry,
                            app = appMap[entry.appKey],
                            iconCache = iconCache,
                            onClick = { app -> onOpenApp(app) },
                            onRemove = { removed ->
                                recents = recents.filterNot { it.appKey == removed.appKey }
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RecentsCard(
    entry: RecentsEntry,
    app: AppInfo?,
    iconCache: IconCache,
    onClick: (AppInfo) -> Unit,
    onRemove: (RecentsEntry) -> Unit,
) {
    var icon by remember(entry.appKey) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(entry.appKey) {
        app?.let { icon = iconCache.iconFor(it, 128) }
    }
    var dismissed by remember(entry.appKey) { mutableStateOf(false) }
    if (dismissed) return

    Column(
        Modifier
            .width(230.dp)
            .aspectRatio(0.62f)
            .clip(RoundedCornerShape(28.dp))
            .background(Brush.verticalGradient(listOf(Color(0xFF2C2C2E), Color(0xFF1C1C1E))))
            .pointerInput(entry.appKey) {
                detectVerticalDragGestures { change, dragAmount ->
                    change.consume()
                    if (dragAmount < -80) {
                        dismissed = true
                        onRemove(entry)
                    }
                }
            }
            .clickable(enabled = app != null) { app?.let(onClick) }
            .padding(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(0.35f))
        icon?.let {
            Image(
                bitmap = it,
                contentDescription = entry.label,
                modifier = Modifier.size(84.dp).clip(RoundedCornerShape(20.dp)),
            )
        } ?: Box(Modifier.size(84.dp).clip(RoundedCornerShape(20.dp)).background(Color(0x33FFFFFF)))
        Spacer(Modifier.height(18.dp))
        IosText(entry.label, fontSize = 16.sp, color = Color.White, fontWeight = FontWeight.Medium, maxLines = 1)
        IosText(
            if (entry.fromSystemStats) "系统记录" else "ASI 记录",
            fontSize = 11.sp,
            color = Color(0x66EBEBF5),
        )
        Spacer(Modifier.weight(0.65f))
        IosText("上滑移除卡片", fontSize = 11.sp, color = Color(0x4DEBEBF5))
    }
}
