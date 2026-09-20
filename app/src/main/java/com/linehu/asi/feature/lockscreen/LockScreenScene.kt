package com.linehu.asi.feature.lockscreen

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.linehu.asi.core.animation.IosSprings
import com.linehu.asi.core.designsystem.IosText
import com.linehu.asi.core.designsystem.rememberMinuteText
import com.linehu.asi.core.gesture.GestureMetrics
import com.linehu.asi.core.gesture.HomeIndicator
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * iOS lock screen: big clock, date, swipe-up-to-unlock with rubber-band
 * damping and a slide-up exit into the springboard.
 */
@Composable
fun LockScreenScene(
    onUnlock: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val timeText = rememberMinuteText()
    val dateText = remember {
        val pattern = android.text.format.DateFormat.getBestDateTimePattern(
            Locale.getDefault(),
            "EEEE MMMMd",
        )
        SimpleDateFormat(pattern, Locale.getDefault()).format(Date())
    }
    val dragOffset = remember { Animatable(0f) }
    var unlocked by remember { mutableStateOf(false) }

    fun tryUnlock(containerHeight: Float) {
        if (unlocked) return
        val committed = -dragOffset.value > containerHeight * GestureMetrics.unlockFraction
        if (committed) {
            unlocked = true
            scope.launch {
                dragOffset.animateTo(-containerHeight * 1.1f, IosSprings.snappy())
                onUnlock()
            }
        } else {
            scope.launch { dragOffset.animateTo(0f, IosSprings.soft()) }
        }
    }

    Box(
        modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragEnd = { tryUnlock(size.height.toFloat()) },
                    onVerticalDrag = { change, dragAmount ->
                        change.consume()
                        val damped = dragAmount * GestureMetrics.lockScreenDragDamp
                        scope.launch {
                            dragOffset.snapTo(
                                (dragOffset.value + damped).coerceIn(-size.height * 0.9f, 0f),
                            )
                        }
                    },
                )
            },
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .graphicsLayer {
                    translationY = dragOffset.value
                    alpha = 1f + (dragOffset.value / (size.height * 1.4f))
                }
                .padding(top = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top,
        ) {
            IosText(
                text = dateText,
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White.copy(alpha = 0.9f),
            )
            Box(Modifier.height(14.dp))
            IosText(
                text = timeText,
                fontSize = 86.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White,
                letterSpacing = (-1).sp,
            )
        }
        // Unlock hint
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 44.dp)
                .graphicsLayer { alpha = (1f + dragOffset.value / 300f).coerceIn(0f, 1f) },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            IosText(
                text = "上滑以解锁",
                fontSize = 15.sp,
                color = Color.White.copy(alpha = 0.75f),
            )
        }
        HomeIndicator(Modifier.align(Alignment.BottomCenter))
    }
}
