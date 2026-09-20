package com.linehu.asi.feature.assistivetouch

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

/**
 * AssistiveTouch-style floating ball. Draggable anywhere; tap opens a menu
 * with shell shortcuts. Doubles as the guaranteed entry to the control /
 * notification centers on devices where the top edge is hogged by the
 * system shade.
 */
@Composable
fun AssistiveTouchOverlay(
    onHome: () -> Unit,
    onSwitcher: () -> Unit,
    onControlCenter: () -> Unit,
    onNotificationCenter: () -> Unit,
    onLock: () -> Unit,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    var x by remember { mutableStateOf(40f) }
    var y by remember { mutableStateOf(400f) }
    val density = LocalDensity.current
    val ballSizePx = with(density) { 52.dp.toPx() }

    Box(modifier.fillMaxSize()) {
        // Menu scrim closes on outside tap
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn(tween(150)),
            exit = fadeOut(tween(150)),
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color(0x33000000))
                    .clickable { expanded = false },
            )
        }

        // Menu around the ball
        AnimatedVisibility(
            visible = expanded,
            enter = scaleIn(initialScale = 0.6f) + fadeIn(tween(150)),
            exit = scaleOut(targetScale = 0.6f) + fadeOut(tween(150)),
            modifier = Modifier.offset { IntOffset(x.roundToInt() - 110, (y - 230).roundToInt()) },
        ) {
            Column(
                Modifier
                    .size(280.dp, 220.dp)
                    .clip(CircleShape)
                    .background(Color(0xE62C2C2E))
                    .padding(20.dp),
                verticalArrangement = Arrangement.SpaceEvenly,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                androidx.compose.foundation.layout.Row(
                    horizontalArrangement = Arrangement.spacedBy(26.dp),
                ) {
                    MenuItem(Icons.Filled.Home, "主屏幕") { expanded = false; onHome() }
                    MenuItem(Icons.Filled.Layers, "多任务") { expanded = false; onSwitcher() }
                    MenuItem(Icons.Filled.Tune, "控制中心") { expanded = false; onControlCenter() }
                }
                androidx.compose.foundation.layout.Row(
                    horizontalArrangement = Arrangement.spacedBy(26.dp),
                ) {
                    MenuItem(Icons.Filled.Notifications, "通知中心") { expanded = false; onNotificationCenter() }
                    MenuItem(Icons.Filled.Lock, "锁定") { expanded = false; onLock() }
                    MenuItem(Icons.Filled.Settings, "设置") { expanded = false; onSettings() }
                }
            }
        }

        // The ball
        Box(
            Modifier
                .offset { IntOffset(x.roundToInt(), y.roundToInt()) }
                .size(52.dp)
                .clip(CircleShape)
                .background(Color(0x9948484A))
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        x = (x + dragAmount.x).coerceIn(0f, size.width - ballSizePx)
                        y = (y + dragAmount.y).coerceIn(0f, size.height - ballSizePx)
                    }
                }
                .clickable { expanded = !expanded },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Filled.Layers,
                contentDescription = "AssistiveTouch",
                tint = Color.White,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

@Composable
private fun MenuItem(icon: ImageVector, label: String, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick).padding(6.dp),
    ) {
        Box(
            Modifier
                .size(50.dp)
                .clip(CircleShape)
                .background(Color(0x33FFFFFF)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = Color.White, modifier = Modifier.size(24.dp))
        }
        androidx.compose.foundation.layout.Spacer(Modifier.size(4.dp))
        com.linehu.asi.core.designsystem.IosText(label, color = Color(0xCCFFFFFF), fontSize = 11.sp)
    }
}
