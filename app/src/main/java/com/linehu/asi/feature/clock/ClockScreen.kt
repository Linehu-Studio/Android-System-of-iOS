package com.linehu.asi.feature.clock

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.SystemClock
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.linehu.asi.core.designsystem.IosStatusBar
import com.linehu.asi.core.designsystem.IosText
import com.linehu.asi.data.timer.SharedTimer
import kotlinx.coroutines.delay

private val ClockBg = Color(0xFF000000)
private val TimerOrange = Color(0xFFFF9F0A)

/** Clock app with two tabs: stopwatch and countdown timer. */
@Composable
fun ClockScreen(sharedTimer: SharedTimer, modifier: Modifier = Modifier) {
    var tab by remember { mutableIntStateOf(0) }
    Column(
        modifier
            .fillMaxSize()
            .background(ClockBg)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        IosStatusBar(contentColor = Color.White)
        Box(Modifier.weight(1f)) {
            when (tab) {
                0 -> StopwatchTab()
                else -> TimerTab(sharedTimer)
            }
        }
        // Tab bar
        Row(
            Modifier.fillMaxWidth().padding(vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            TabLabel("秒表", tab == 0) { tab = 0 }
            TabLabel("计时器", tab == 1) { tab = 1 }
        }
    }
}

@Composable
private fun TabLabel(text: String, active: Boolean, onClick: () -> Unit) {
    IosText(
        text = text,
        fontSize = 18.sp,
        color = if (active) TimerOrange else Color(0x99EBEBF5),
        modifier = Modifier.clickable(onClick = onClick).padding(8.dp),
    )
}

// ---------- Stopwatch ----------

private class Lap(val index: Int, val elapsedAt: Long)

@Composable
private fun StopwatchTab() {
    var running by remember { mutableStateOf(false) }
    var baseElapsed by remember { mutableLongStateOf(0L) } // ms accumulated while paused
    var startAt by remember { mutableLongStateOf(0L) } // elapsedRealtime at last resume
    var laps by remember { mutableStateOf(listOf<Lap>()) }
    var nowTick by remember { mutableLongStateOf(0L) }

    LaunchedEffect(running) {
        while (true) {
            nowTick = SystemClock.elapsedRealtime()
            delay(31)
        }
    }

    fun currentElapsed(): Long =
        baseElapsed + if (running) nowTick - startAt else 0L

    val elapsed = currentElapsed()
    val lapCount = laps.size

    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        IosText(
            text = formatStopwatch(elapsed),
            fontSize = 76.sp,
            fontWeight = FontWeight.Thin,
            color = Color.White,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 48.dp, bottom = 24.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        // Laps
        LazyColumn(Modifier.weight(1f)) {
            items(laps) { lap ->
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    IosText("计次 ${laps.size - lap.index + 1}", color = Color(0x99EBEBF5), fontSize = 16.sp)
                    IosText(formatStopwatch(lap.elapsedAt), color = Color.White, fontSize = 16.sp)
                }
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(vertical = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            ActionCircle(
                label = if (running) "计次" else "还原",
                color = Color(0xFF333336),
                textColor = Color.White,
            ) {
                if (running) {
                    laps = listOf(Lap(lapCount + 1, elapsed)) + laps
                } else {
                    baseElapsed = 0; startAt = 0; laps = emptyList()
                }
            }
            ActionCircle(
                label = if (running) "停止" else "启动",
                color = if (running) Color(0xFF3A1714) else Color(0xFF1A2E1A),
                textColor = if (running) Color(0xFFFF453A) else Color(0xFF30D158),
            ) {
                if (running) {
                    baseElapsed += SystemClock.elapsedRealtime() - startAt
                    running = false
                } else {
                    startAt = SystemClock.elapsedRealtime()
                    running = true
                }
            }
        }
    }
}

// ---------- Timer ----------

@Composable
private fun TimerTab(sharedTimer: SharedTimer) {
    val context = LocalContext.current
    val timerState by sharedTimer.state.collectAsStateWithLifecycle()
    var hours by remember { mutableIntStateOf(0) }
    var minutes by remember { mutableIntStateOf(5) }
    var seconds by remember { mutableIntStateOf(0) }
    var nowTick by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var notifiedFinish by remember { mutableStateOf(false) }

    val notifPermission = remember {
        mutableStateOf(
            Build.VERSION.SDK_INT < 33 ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> notifPermission.value = granted }

    LaunchedEffect(Unit) {
        while (true) {
            nowTick = System.currentTimeMillis()
            delay(200)
        }
    }

    val remaining = timerState.remainingAt(nowTick)
    // Fire a local notification when the countdown hits zero.
    LaunchedEffect(remaining, timerState.running) {
        if (timerState.running && remaining <= 0 && !notifiedFinish) {
            notifiedFinish = true
            if (notifPermission.value) notifyTimerFinished(context)
            sharedTimer.cancel()
        }
        if (timerState.running && remaining > 0) notifiedFinish = false
    }

    Column(
        Modifier.fillMaxSize().padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(24.dp))
        if (timerState.running || timerState.pausedRemainingMs > 0) {
            CountdownRing(
                remainingMs = remaining,
                totalMs = timerState.totalMs,
            )
            Spacer(Modifier.height(32.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                ActionCircle("取消", Color(0xFF333336), Color.White) { sharedTimer.cancel() }
                ActionCircle(
                    label = if (timerState.running) "暂停" else "继续",
                    color = Color(0xFF1A2E1A),
                    textColor = Color(0xFF30D158),
                ) {
                    if (timerState.running) sharedTimer.pause() else sharedTimer.resume()
                }
            }
        } else {
            IosText(
                text = formatCountdown(hours * 3600_000L + minutes * 60_000L + seconds * 1000L),
                fontSize = 64.sp,
                fontWeight = FontWeight.Thin,
                color = Color.White,
            )
            Spacer(Modifier.height(32.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                TimePickerColumn("小时", 24) { hours = it }
                TimePickerColumn("分钟", 60) { minutes = it }
                TimePickerColumn("秒", 60) { seconds = it }
            }
            Spacer(Modifier.height(32.dp))
            ActionCircle(
                label = "开始",
                color = Color(0xFF1A2E1A),
                textColor = Color(0xFF30D158),
            ) {
                if (Build.VERSION.SDK_INT >= 33 && !notifPermission.value) {
                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
                val total = hours * 3600_000L + minutes * 60_000L + seconds * 1000L
                if (total > 0) {
                    notifiedFinish = false
                    sharedTimer.start(total)
                }
            }
        }
    }
}

@Composable
private fun TimePickerColumn(label: String, range: Int, initial: Int = 0, onPick: (Int) -> Unit) {
    var value by remember(label) { mutableIntStateOf(initial) }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .size(width = 72.dp, height = 120.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF1C1C1E))
                .clickable {
                    value = (value + 1) % range
                    onPick(value)
                },
            contentAlignment = Alignment.Center,
        ) {
            IosText(value.toString().padStart(2, '0'), fontSize = 32.sp, color = Color.White)
        }
        Spacer(Modifier.height(6.dp))
        IosText(label, fontSize = 13.sp, color = Color(0x99EBEBF5))
    }
}

@Composable
private fun CountdownRing(remainingMs: Long, totalMs: Long) {
    val fraction = if (totalMs > 0) remainingMs.toFloat() / totalMs else 0f
    Box(contentAlignment = Alignment.Center) {
        androidx.compose.foundation.Canvas(Modifier.size(260.dp)) {
            val stroke = 14.dp.toPx()
            drawArc(
                color = Color(0x33FF9F0A),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = androidx.compose.ui.graphics.drawscope.Stroke(stroke),
            )
            drawArc(
                color = TimerOrange,
                startAngle = -90f,
                sweepAngle = 360f * fraction,
                useCenter = false,
                style = androidx.compose.ui.graphics.drawscope.Stroke(stroke),
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            IosText(
                formatCountdown(remainingMs),
                fontSize = 56.sp,
                fontWeight = FontWeight.Thin,
                color = Color.White,
            )
        }
    }
}

@Composable
private fun ActionCircle(
    label: String,
    color: Color,
    textColor: Color,
    onClick: () -> Unit,
) {
    Box(
        Modifier
            .size(84.dp)
            .clip(CircleShape)
            .background(color)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        IosText(label, fontSize = 18.sp, color = textColor)
    }
}

private fun formatStopwatch(ms: Long): String {
    val total = ms / 10
    val cs = total % 100
    val s = (total / 100) % 60
    val m = (total / 6000) % 60
    val h = total / 360000
    return if (h > 0) "%d:%02d:%02d.%02d".format(h, m, s, cs)
    else "%02d:%02d.%02d".format(m, s, cs)
}

private fun formatCountdown(ms: Long): String {
    val total = ms / 1000
    val s = total % 60
    val m = (total / 60) % 60
    val h = total / 3600
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}

private fun notifyTimerFinished(context: Context) {
    val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    if (Build.VERSION.SDK_INT >= 26) {
        nm.createNotificationChannel(
            NotificationChannel("timer", "计时器", NotificationManager.IMPORTANCE_HIGH),
        )
    }
    val builder = androidx.core.app.NotificationCompat.Builder(context, "timer")
        .setSmallIcon(android.R.drawable.ic_dialog_info)
        .setContentTitle("计时器")
        .setContentText("时间到！")
        .setAutoCancel(true)
    nm.notify(1001, builder.build())
}
