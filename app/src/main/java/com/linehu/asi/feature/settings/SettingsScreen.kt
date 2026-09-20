package com.linehu.asi.feature.settings

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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.linehu.asi.core.designsystem.IosStatusBar
import com.linehu.asi.core.designsystem.IosText
import com.linehu.asi.data.settings.AsiSettings
import com.linehu.asi.data.wallpaper.BUILTIN_WALLPAPERS
import com.linehu.asi.data.settings.FreeformMode
import com.linehu.asi.data.settings.SettingsRepository
import com.linehu.asi.data.settings.ThemeMode
import kotlinx.coroutines.launch

private val SettingsBg = Color(0xFFF2F2F7)
private val SettingsCard = Color(0xFFFFFFFF)
private val SettingsBlue = Color(0xFF007AFF)

/** iOS-style grouped settings list. */
@Composable
fun SettingsScreen(
    repository: SettingsRepository,
    modifier: Modifier = Modifier,
) {
    val settings by repository.settings.collectAsStateWithLifecycle(initialValue = AsiSettings())
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val gradientWallpapers = remember { BUILTIN_WALLPAPERS }

    Column(
        modifier
            .fillMaxSize()
            .background(SettingsBg)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        IosStatusBar(contentColor = Color.Black)
        IosText(
            "设置",
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            GroupCard {
                SettingRow("外观") {
                    Segmented(
                        options = listOf("跟随系统" to ThemeMode.SYSTEM, "浅色" to ThemeMode.LIGHT, "深色" to ThemeMode.DARK),
                        selected = settings.themeMode,
                        onSelect = { mode -> scope.launch { repository.setThemeMode(mode) } },
                    )
                }
                SettingRow("图标风格") {
                    Segmented(
                        options = listOf("iOS 化" to "ios", "原生" to "native"),
                        selected = if (settings.iconStyle == com.linehu.asi.data.settings.IconStyle.IOS) "ios" else "native",
                        onSelect = { value ->
                            scope.launch {
                                repository.setIconStyle(
                                    if (value == "ios") com.linehu.asi.data.settings.IconStyle.IOS
                                    else com.linehu.asi.data.settings.IconStyle.NATIVE,
                                )
                            }
                        },
                    )
                }
            }
            GroupTitle("多窗口")
            GroupCard {
                SettingRow("自由窗口") {
                    Segmented(
                        options = listOf("自动" to FreeformMode.AUTO, "开" to FreeformMode.ON, "关" to FreeformMode.OFF),
                        selected = settings.freeformMode,
                        onSelect = { mode -> scope.launch { repository.setFreeformMode(mode) } },
                    )
                }
                InfoText("设备支持时（DeX/平板/部分ROM），应用将以自由窗口启动，类似 iPad 台前调度。")
            }
            GroupTitle("壁纸")
            GroupCard {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                    IosText("选择壁纸", fontSize = 16.sp, color = Color.Black)
                    Spacer(Modifier.height(10.dp))
                    androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                        columns = androidx.compose.foundation.lazy.grid.GridCells.Fixed(3),
                        modifier = Modifier.fillMaxWidth().height(240.dp),
                        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
                        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
                        userScrollEnabled = false,
                    ) {
                        items(gradientWallpapers.size + 1) { index ->
                            val selected = settings.wallpaperId == index
                            val wallpaper = gradientWallpapers.getOrNull(index - 1)
                            Box(
                                Modifier
                                    .height(96.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(
                                        if (wallpaper != null) {
                                            androidx.compose.ui.graphics.Brush.verticalGradient(
                                                wallpaper.colors.map { androidx.compose.ui.graphics.Color(it) },
                                            )
                                        } else {
                                            androidx.compose.ui.graphics.Brush.verticalGradient(
                                                listOf(Color(0xFF3A3A3C), Color(0xFF1C1C1E)),
                                            )
                                        },
                                    )
                                    .clickable {
                                        scope.launch { repository.setWallpaperId(index) }
                                    }
                                    .padding(6.dp),
                                contentAlignment = Alignment.BottomStart,
                            ) {
                                if (selected) {
                                    Box(
                                        Modifier
                                            .size(18.dp)
                                            .clip(CircleShape)
                                            .background(SettingsBlue),
                                    ) {
                                        Icon(
                                            androidx.compose.material.icons.Icons.Filled.Check,
                                            null,
                                            tint = Color.White,
                                            modifier = Modifier.size(14.dp),
                                        )
                                    }
                                }
                                IosText(
                                    if (wallpaper != null) wallpaper.name else "跟随系统",
                                    fontSize = 12.sp,
                                    color = Color.White,
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    InfoText("渐变壁纸也可以\"应用为系统壁纸\"，让其它应用启动时视觉连续。")
                }
            }
            GroupTitle("锁屏")
            GroupCard {
                SettingRow("回到桌面后重新上锁") {
                    Segmented(
                        options = listOf("从不" to 0, "1分钟" to 1, "5分钟" to 5),
                        selected = settings.lockTimeoutMinutes,
                        onSelect = { v -> scope.launch { repository.setLockTimeout(v) } },
                    )
                }
            }
            GroupTitle("权限")
            GroupCard {
                LinkRow("通知访问权限", "读取系统通知显示在通知中心") {
                    context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                }
                LinkRow("使用情况访问", "增强多任务记录（可选）") {
                    context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                }
                LinkRow("修改系统设置", "控制中心亮度滑块需要") {
                    context.startActivity(Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS))
                }
            }
            GroupTitle("关于")
            GroupCard {
                InfoText("ASI (Android System of iOS) v0.1.0\niOS 18 风格桌面环境 for Android\n\n限制说明：无法真正虚拟化 iOS；多任务为自记录 + 可选使用情况统计；通知中心需授权通知访问。")
            }
            Spacer(Modifier.height(30.dp))
        }
    }
}

@Composable
private fun GroupTitle(text: String) {
    IosText(
        text,
        fontSize = 13.sp,
        color = Color(0x993C3C43),
        modifier = Modifier.padding(start = 16.dp, top = 18.dp, bottom = 6.dp),
    )
}

@Composable
private fun GroupCard(content: @Composable () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SettingsCard),
    ) {
        content()
    }
}

@Composable
private fun SettingRow(label: String, trailing: @Composable () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IosText(label, fontSize = 16.sp, color = Color.Black)
        Spacer(Modifier.weight(1f))
        trailing()
    }
}

@Composable
private fun LinkRow(label: String, subtitle: String, onClick: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        IosText(label, fontSize = 16.sp, color = SettingsBlue)
        IosText(subtitle, fontSize = 13.sp, color = Color(0x993C3C43))
    }
}

@Composable
private fun InfoText(text: String) {
    IosText(
        text,
        fontSize = 13.sp,
        color = Color(0x993C3C43),
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
    )
}

/** iOS segmented control. */
@Composable
fun <T> Segmented(
    options: List<Pair<String, T>>,
    selected: T,
    onSelect: (T) -> Unit,
) {
    Row(
        Modifier
            .clip(RoundedCornerShape(9.dp))
            .background(Color(0x1F3C3C43))
            .padding(2.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        options.forEach { (label, value) ->
            val active = value == selected
            Box(
                Modifier
                    .clip(RoundedCornerShape(7.dp))
                    .background(if (active) Color.White else Color.Transparent)
                    .clickable { onSelect(value) }
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            ) {
                IosText(
                    label,
                    fontSize = 13.sp,
                    color = if (active) Color.Black else Color(0x993C3C43),
                )
            }
        }
    }
}

/** iOS-style toggle switch. */
@Composable
fun IosSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier
            .width(51.dp)
            .height(31.dp)
            .clip(CircleShape)
            .background(if (checked) SettingsBlue else Color(0x1F3C3C43))
            .clickable { onCheckedChange(!checked) },
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            Modifier
                .padding(2.dp)
                .width(27.dp)
                .height(27.dp)
                .clip(CircleShape)
                .background(Color.White)
                .align(if (checked) Alignment.CenterEnd else Alignment.CenterStart),
        )
    }
}
