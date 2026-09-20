package com.linehu.asi.core.designsystem

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.os.BatteryManager
import android.text.format.DateFormat
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.roundToInt

data class StatusBarState(
    val timeText: String = "",
    val batteryPercent: Int = 100,
    val charging: Boolean = false,
    val wifiConnected: Boolean = false,
)

/**
 * Self-drawn iOS status bar. The system status bar is hidden — we render time
 * (left) and wifi + battery (right) ourselves, exactly where iOS puts them.
 */
@Composable
fun IosStatusBar(
    modifier: Modifier = Modifier,
    height: Dp = 44.dp,
    contentColor: Color = Color.White,
    state: StatusBarState = rememberStatusBarState(),
    centerSlot: @Composable () -> Unit = {},
) {
    val label = LocalIosColors.current
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 26.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier.size(width = 160.dp, height = height),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IosText(
                text = state.timeText,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = contentColor,
            )
            Spacer(Modifier.weight(1f))
            if (state.wifiConnected) WifiIcon(contentColor, Modifier.size(17.dp))
            Spacer(Modifier.width(5.dp))
            BatteryIcon(
                percent = state.batteryPercent,
                charging = state.charging,
                bodyColor = contentColor,
                modifier = Modifier.size(width = 25.dp, height = 12.dp),
            )
        }
        Box(contentAlignment = Alignment.Center) { centerSlot() }
    }
}

/** Minimal iOS-style label text used across system surfaces. */
@Composable
fun IosText(
    text: String,
    modifier: Modifier = Modifier,
    fontSize: androidx.compose.ui.unit.TextUnit = 17.sp,
    fontWeight: FontWeight = FontWeight.Normal,
    color: Color = LocalIosColors.current.label,
    letterSpacing: androidx.compose.ui.unit.TextUnit = 0.sp,
    textAlign: TextAlign? = null,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
) {
    androidx.compose.material3.Text(
        text = text,
        modifier = modifier,
        color = color,
        fontSize = fontSize,
        fontWeight = fontWeight,
        letterSpacing = letterSpacing,
        textAlign = textAlign,
        maxLines = maxLines,
        overflow = overflow,
        fontFamily = IosTypography.sfProFamily,
    )
}

@Composable
fun rememberStatusBarState(): StatusBarState {
    val context = LocalContext.current
    val state = remember { mutableStateOf(readBattery(context)) }

    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                if (intent.action == Intent.ACTION_BATTERY_CHANGED ||
                    intent.action == Intent.ACTION_POWER_CONNECTED ||
                    intent.action == Intent.ACTION_POWER_DISCONNECTED
                ) {
                    state.value = readBattery(ctx)
                }
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_BATTERY_CHANGED)
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
        }
        ContextCompatRegister(context, receiver, filter)
        onDispose { context.unregisterReceiver(receiver) }
    }
    val wifi = rememberWifiConnected()
    val timeText = rememberMinuteText()
    return state.value.copy(wifiConnected = wifi, timeText = timeText)
}

private fun ContextCompatRegister(context: Context, receiver: BroadcastReceiver, filter: IntentFilter) {
    androidx.core.content.ContextCompat.registerReceiver(
        context,
        receiver,
        filter,
        androidx.core.content.ContextCompat.RECEIVER_EXPORTED,
    )
}

private fun readBattery(context: Context): StatusBarState {
    val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
    val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
    val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
    val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
        status == BatteryManager.BATTERY_STATUS_FULL
    val percent = if (level >= 0 && scale > 0) (level * 100f / scale).roundToInt() else 100
    return StatusBarState(batteryPercent = percent, charging = charging)
}

/** Ticks so the clock never shows a stale minute. */
@Composable
fun rememberMinuteText(): String {
    val context = LocalContext.current
    val is24h = DateFormat.is24HourFormat(context)
    var text by remember {
        mutableStateOf(currentTimeText(is24h))
    }
    androidx.compose.runtime.LaunchedEffect(is24h) {
        while (true) {
            val next = Calendar.getInstance().apply {
                add(Calendar.MINUTE, 1)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            kotlinx.coroutines.delay(next.timeInMillis - System.currentTimeMillis())
            text = currentTimeText(is24h)
        }
    }
    return text
}

private fun currentTimeText(is24h: Boolean): String {
    val pattern = if (is24h) "HH:mm" else "h:mm"
    return SimpleDateFormat(pattern, Locale.getDefault()).format(Calendar.getInstance().time)
}

@Composable
private fun rememberWifiConnected(): Boolean {
    val context = LocalContext.current
    var connected by remember { mutableStateOf(checkWifi(context)) }
    androidx.compose.runtime.DisposableEffect(context) {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
        val callback = object : android.net.ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: android.net.Network) {
                connected = checkWifi(context)
            }

            override fun onLost(network: android.net.Network) {
                connected = checkWifi(context)
            }
        }
        cm.registerNetworkCallback(
            android.net.NetworkRequest.Builder()
                .addTransportType(android.net.NetworkCapabilities.TRANSPORT_WIFI)
                .build(),
            callback,
        )
        onDispose { cm.unregisterNetworkCallback(callback) }
    }
    return connected
}

private fun checkWifi(context: Context): Boolean {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
    val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
    return caps.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI)
}

@Composable
private fun WifiIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val arcs = 3
        val stroke = Stroke(width = size.width * 0.11f, cap = StrokeCap.Round)
        for (i in 1..arcs) {
            val radius = size.minDimension * 0.22f * i
            drawArc(
                color = color,
                startAngle = 215f,
                sweepAngle = 110f,
                useCenter = false,
                topLeft = androidx.compose.ui.geometry.Offset(
                    size.width / 2 - radius,
                    size.height * 0.72f - radius,
                ),
                size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
                style = stroke,
            )
        }
        drawCircle(
            color = color,
            radius = size.width * 0.06f,
            center = androidx.compose.ui.geometry.Offset(size.width / 2, size.height * 0.72f),
        )
    }
}

@Composable
private fun BatteryIcon(
    percent: Int,
    charging: Boolean,
    bodyColor: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val corner = h * 0.22f
        // Shell
        drawRoundRect(
            color = bodyColor.copy(alpha = 0.45f),
            size = androidx.compose.ui.geometry.Size(w - h * 0.28f, h),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(corner, corner),
            style = Stroke(width = h * 0.13f),
        )
        // Nub
        drawRoundRect(
            color = bodyColor.copy(alpha = 0.45f),
            size = androidx.compose.ui.geometry.Size(h * 0.16f, h * 0.5f),
            topLeft = androidx.compose.ui.geometry.Offset(w - h * 0.22f, h * 0.25f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(h * 0.08f, h * 0.08f),
        )
        // Fill
        val fillFraction = (percent / 100f).coerceIn(0f, 1f)
        val fillColor = when {
            charging -> Color(0xFF30D158)
            percent <= 20 -> Color(0xFFFF453A)
            else -> bodyColor
        }
        val inset = h * 0.16f
        val bodyW = w - h * 0.28f - inset * 2
        drawRoundRect(
            color = fillColor,
            size = androidx.compose.ui.geometry.Size(
                (bodyW * fillFraction).coerceAtLeast(h * 0.2f),
                h - inset * 2,
            ),
            topLeft = androidx.compose.ui.geometry.Offset(inset, inset),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(corner / 2, corner / 2),
        )
        // Lightning bolt when charging
        if (charging) {
            val path = Path().apply {
                moveTo(w * 0.44f, h * 0.18f)
                lineTo(w * 0.30f, h * 0.56f)
                lineTo(w * 0.42f, h * 0.56f)
                lineTo(w * 0.36f, h * 0.84f)
                lineTo(w * 0.56f, h * 0.44f)
                lineTo(w * 0.43f, h * 0.44f)
                close()
            }
            drawPath(path, Color(0xFF1C1C1E))
        }
    }
}
