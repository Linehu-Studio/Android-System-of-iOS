package com.linehu.asi.feature.dynamicisland

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.linehu.asi.core.animation.IosSprings
import com.linehu.asi.core.designsystem.IosText
import com.linehu.asi.data.timer.SharedTimer

/**
 * iOS Dynamic Island. Hugs the punch-hole camera when there is one; floats
 * centered below the status bar otherwise. Content priority:
 * app-launch flash > running countdown > charging > idle pill.
 */
@Composable
fun DynamicIsland(
    flashLabel: String?,
    timerState: SharedTimer.TimerState,
    charging: Boolean,
    batteryPercent: Int,
    cutoutWidthPx: Float?,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = androidx.compose.ui.platform.LocalDensity.current
    val cutoutWidth = cutoutWidthPx?.let { with(density) { it.toDp() } }

    val expanded = flashLabel != null || timerState.running || charging
    val collapsedWidth = (cutoutWidth?.plus(16.dp))?.coerceAtLeast(96.dp) ?: 96.dp
    val width by animateDpAsState(
        targetValue = if (expanded) 236.dp else collapsedWidth,
        animationSpec = IosSprings.bouncy(),
        label = "island-width",
    )
    val height by animateDpAsState(
        targetValue = if (expanded) 42.dp else 30.dp,
        animationSpec = IosSprings.bouncy(),
        label = "island-height",
    )

    Box(
        modifier
            .width(width)
            .height(height)
            .clip(RoundedCornerShape(50))
            .background(Color.Black)
            .clickable(onClick = onTap),
        contentAlignment = Alignment.Center,
    ) {
        AnimatedContent(
            targetState = when {
                flashLabel != null -> Mode.FLASH
                timerState.running -> Mode.TIMER
                charging -> Mode.CHARGE
                else -> Mode.IDLE
            },
            transitionSpec = { fadeIn(tween(150)) togetherWith fadeOut(tween(120)) },
            label = "island-content",
        ) { mode ->
            when (mode) {
                Mode.FLASH -> IslandRow(label = flashLabel.orEmpty(), trailing = "")
                Mode.TIMER -> {
                    val now = System.currentTimeMillis()
                    val remaining = timerState.remainingAt(now)
                    val totalSec = timerState.totalMs / 1000
                    val rem = remaining / 1000
                    IslandRow(
                        label = "计时器  %d:%02d".format(rem / 60, rem % 60),
                        trailing = "共 ${totalSec / 60} 分",
                    )
                }
                Mode.CHARGE -> IslandRow(label = "充电中", trailing = "$batteryPercent%")
                Mode.IDLE -> Box(Modifier.size(0.dp))
            }
        }
    }
}

private enum class Mode { FLASH, TIMER, CHARGE, IDLE }

@Composable
private fun IslandRow(label: String, trailing: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IosText(label, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color.White)
        Spacer(Modifier.weight(1f))
        if (trailing.isNotEmpty()) {
            IosText(trailing, fontSize = 13.sp, color = Color(0xFFFFD60A))
        }
    }
}
