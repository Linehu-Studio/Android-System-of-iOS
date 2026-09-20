package com.linehu.asi.feature.controlcenter

import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.AirplanemodeActive
import androidx.compose.material.icons.filled.DoNotDisturbOn
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.linehu.asi.core.designsystem.IosText
import com.linehu.asi.core.designsystem.LocalIosColors
import com.linehu.asi.model.AppId
import com.linehu.asi.system.BrightnessController
import com.linehu.asi.system.TorchController
import com.linehu.asi.system.VolumeController
import kotlin.math.roundToInt

private val CcBlue = Color(0xFF0A84FF)

/**
 * iOS 18 control center. Tiles are honest about what Android allows:
 * connectivity toggles deep-link into system panels; flashlight/volume/
 * brightness are directly controlled.
 */
@Composable
fun ControlCenterScreen(
    torch: TorchController,
    brightness: BrightnessController,
    volume: VolumeController,
    onOpenBuiltin: (AppId) -> Unit,
    onLock: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val colors = LocalIosColors.current
    var torchOn by remember { mutableStateOf(torch.on) }

    fun openConnectivityPanel() {
        runCatching {
            val intent = if (Build.VERSION.SDK_INT >= 29) {
                Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY)
            } else {
                Intent(Settings.ACTION_WIFI_SETTINGS)
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }

    fun openBluetoothPanel() {
        runCatching {
            val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }

    fun openDndPanel() {
        runCatching {
            val intent = Intent("android.settings.ZEN_MODE_SETTINGS")
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }

    Column(
        modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .padding(top = 64.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            // Connectivity 2×2
            Box(
                Modifier
                    .weight(1.4f)
                    .aspectRatio(1.55f)
                    .clip(RoundedCornerShape(22.dp))
                    .background(Color(0x33FFFFFF)),
            ) {
                Column(Modifier.fillMaxSize().padding(14.dp)) {
                    Row(Modifier.weight(1f)) {
                        RoundTile(Icons.Filled.Wifi, "Wi-Fi", Modifier.weight(1f)) { openConnectivityPanel() }
                        RoundTile(Icons.Filled.Bluetooth, "蓝牙", Modifier.weight(1f)) { openBluetoothPanel() }
                    }
                    Row(Modifier.weight(1f)) {
                        RoundTile(Icons.Filled.AirplanemodeActive, "飞行", Modifier.weight(1f)) { openConnectivityPanel() }
                        RoundTile(Icons.Filled.DoNotDisturbOn, "勿扰", Modifier.weight(1f)) { openDndPanel() }
                    }
                }
            }
            // Rotation + music-ish card
            Box(
                Modifier
                    .weight(1f)
                    .aspectRatio(1.1f)
                    .clip(RoundedCornerShape(22.dp))
                    .background(Color(0x33FFFFFF))
                    .clickable { openDndPanel() },
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.ScreenRotation, null, tint = Color.White)
                    Spacer(Modifier.height(6.dp))
                    IosText("系统设置", fontSize = 12.sp, color = Color.White)
                }
            }
        }

        // Sliders
        Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            VerticalSlider(
                icon = { Icon(Icons.Filled.Brightness6, null, tint = Color.White) },
                initial = brightness.brightness(),
                canControl = brightness.canWriteBrightness(),
                hint = "需要\"修改系统设置\"权限",
                onSet = { brightness.setBrightness(it) },
                modifier = Modifier.weight(1f).fillMaxSize(),
            )
            VerticalSlider(
                icon = { Icon(Icons.Filled.VolumeUp, null, tint = Color.White) },
                initial = volume.volume(),
                canControl = true,
                hint = "",
                onSet = { volume.setVolume(it) },
                modifier = Modifier.weight(1f).fillMaxSize(),
            )
        }

        // Bottom tiles
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            SmallTile(Icons.Filled.FlashlightOn, "手电", torchOn) {
                torchOn = torch.toggle()
            }
            SmallTile(Icons.Filled.Timer, "计时器", false) { onOpenBuiltin(AppId.CLOCK) }
            SmallTile(Icons.Filled.Calculate, "计算器", false) { onOpenBuiltin(AppId.CALCULATOR) }
            SmallTile(Icons.Filled.Lock, "锁定", false) { onLock() }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun RoundTile(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier.fillMaxSize().padding(4.dp).clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(CcBlue)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = Color.White, modifier = Modifier.size(26.dp))
        }
        Spacer(Modifier.height(4.dp))
        IosText(label, fontSize = 11.sp, color = Color(0xCCFFFFFF))
    }
}

@Composable
private fun SmallTile(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, active: Boolean, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick).padding(6.dp),
    ) {
        Box(
            Modifier
                .size(58.dp)
                .clip(CircleShape)
                .background(if (active) Color.White else Color(0x33FFFFFF)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = if (active) Color.Black else Color.White)
        }
        Spacer(Modifier.height(4.dp))
        IosText(label, fontSize = 11.sp, color = Color(0xCCFFFFFF))
    }
}

/** iOS-style tall vertical slider with drag-anywhere control. */
@Composable
private fun VerticalSlider(
    icon: @Composable () -> Unit,
    initial: Float,
    canControl: Boolean,
    hint: String,
    onSet: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    var fraction by remember { mutableFloatStateOf(initial.coerceIn(0f, 1f)) }
    Box(
        modifier
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0x33FFFFFF))
            .pointerInput(canControl) {
                if (!canControl) return@pointerInput
                detectVerticalDragGestures { change, dragAmount ->
                    change.consume()
                    fraction = (fraction - dragAmount / size.height).coerceIn(0f, 1f)
                    onSet(fraction)
                }
            },
    ) {
        // Fill from bottom
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .fillMaxHeight(fraction)
                .background(Color(0xE6FFFFFF)),
        )
        Column(
            Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            icon()
            Spacer(Modifier.height(8.dp))
            IosText("${(fraction * 100).roundToInt()}%", fontSize = 13.sp, color = Color.Black)
            if (!canControl) {
                Spacer(Modifier.height(8.dp))
                IosText(hint, fontSize = 10.sp, color = Color(0xCC3C3C43))
            }
        }
    }
}
