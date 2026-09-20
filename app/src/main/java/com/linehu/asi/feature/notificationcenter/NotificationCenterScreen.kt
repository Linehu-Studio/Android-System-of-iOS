package com.linehu.asi.feature.notificationcenter

import android.content.Intent
import android.provider.Settings
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.linehu.asi.core.designsystem.IosText
import com.linehu.asi.data.notifications.NotificationRepository

/**
 * iOS notification center. Shows the permission-guide card until the user
 * grants notification access; after that, live cards with per-app grouping
 * and a clear-all action.
 */
@Composable
fun NotificationCenterScreen(
    repository: NotificationRepository,
    onClearAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val notifications by repository.notifications.collectAsStateWithLifecycle()
    val listenerEnabled = NotificationRepository.isListenerEnabled(context)

    Column(
        modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .padding(top = 64.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IosText("通知中心", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
            if (listenerEnabled && notifications.isNotEmpty()) {
                IosText(
                    "全部清除",
                    fontSize = 15.sp,
                    color = Color(0xFFFF453A),
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(onClick = onClearAll)
                        .padding(6.dp),
                )
            }
        }
        Spacer(Modifier.height(12.dp))

        if (!listenerEnabled) {
            PermissionGuide()
        } else if (notifications.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                IosText("没有新通知", color = Color(0x66EBEBF5), fontSize = 16.sp)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(notifications, key = { it.key }) { n ->
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color(0xE62C2C2E))
                            .clickable {
                                runCatching {
                                    context.packageManager.getLaunchIntentForPackage(n.packageName)
                                        ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        ?.let { context.startActivity(it) }
                                }
                            }
                            .padding(14.dp),
                    ) {
                        IosText(
                            n.appLabel,
                            fontSize = 12.sp,
                            color = Color(0x99EBEBF5),
                        )
                        if (n.title.isNotBlank()) {
                            IosText(n.title, fontSize = 15.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
                        }
                        if (n.text.isNotBlank()) {
                            IosText(n.text, fontSize = 14.sp, color = Color(0xE6EBEBF5))
                        }
                        IosText(
                            relativeTime(n.timestamp),
                            fontSize = 11.sp,
                            color = Color(0x66EBEBF5),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionGuide() {
    val context = LocalContext.current
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xE62C2C2E))
            .padding(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        IosText("开启通知访问", fontSize = 17.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))
        IosText(
            "ASI 需要通知访问权限才能显示系统通知。\n数据仅在本机使用，不会上传。",
            fontSize = 13.sp,
            color = Color(0x99EBEBF5),
        )
        Spacer(Modifier.height(12.dp))
        Box(
            Modifier
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFF0A84FF))
                .clickable {
                    runCatching {
                        context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                    }
                }
                .padding(horizontal = 20.dp, vertical = 10.dp),
        ) {
            IosText("前往系统设置", fontSize = 15.sp, color = Color.White)
        }
    }
}

private fun relativeTime(timestamp: Long): String {
    val diffMin = (System.currentTimeMillis() - timestamp) / 60000
    return when {
        diffMin < 1 -> "刚刚"
        diffMin < 60 -> "$diffMin 分钟前"
        diffMin < 60 * 24 -> "${diffMin / 60} 小时前"
        else -> "${diffMin / (60 * 24)} 天前"
    }
}
