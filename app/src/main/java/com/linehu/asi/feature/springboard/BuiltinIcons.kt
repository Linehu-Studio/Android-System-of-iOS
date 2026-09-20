package com.linehu.asi.feature.springboard

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import com.linehu.asi.core.designsystem.squirclePath
import com.linehu.asi.model.AppId
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Built-in app icons drawn in code — zero image assets, crisp at any density,
 * and they always match the current squircle mask.
 */
@Composable
fun BuiltinAppIcon(appId: AppId, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        clipPath(squirclePath(size)) {
            when (appId) {
                AppId.CALCULATOR -> drawCalculator()
                AppId.CLOCK -> drawClock()
                AppId.NOTES -> drawNotes()
                AppId.WEATHER -> drawWeather()
                AppId.PHOTOS -> drawPhotos()
                AppId.SETTINGS -> drawSettings()
            }
        }
    }
}

private fun DrawScope.drawCalculator() {
    drawRect(Color(0xFF1C1C1E))
    // Display
    drawRoundRect(
        Color(0xFF2C2C2E),
        topLeft = Offset(size.width * 0.10f, size.height * 0.09f),
        size = Size(size.width * 0.80f, size.height * 0.16f),
        cornerRadius = CornerRadius(size.width * 0.04f),
    )
    // Button grid: 3 gray columns + 1 orange column
    val cols = 4
    val rows = 4
    val gridW = size.width * 0.80f
    val gridH = size.height * 0.58f
    val ox = size.width * 0.10f
    val oy = size.height * 0.32f
    val gap = size.width * 0.035f
    val bw = (gridW - gap * (cols - 1)) / cols
    val bh = (gridH - gap * (rows - 1)) / rows
    for (r in 0 until rows) {
        for (c in 0 until cols) {
            val color = when (c) {
                3 -> Color(0xFFFF9F0A)
                0 -> if (r == 0) Color(0xFFD4D4D2) else Color(0xFF48484A)
                else -> Color(0xFF636366)
            }
            drawRoundRect(
                color,
                topLeft = Offset(ox + c * (bw + gap), oy + r * (bh + gap)),
                size = Size(bw, bh),
                cornerRadius = CornerRadius(bw * 0.5f),
            )
        }
    }
}

private fun DrawScope.drawClock() {
    drawRect(Color.Black)
    val r = size.minDimension * 0.40f
    val c = center
    drawCircle(Color.White, radius = r, center = c)
    // Ticks
    for (i in 0 until 12) {
        val a = i * PI / 6
        val inner = r * 0.86f
        drawLine(
            Color.Black,
            start = Offset(c.x + inner * cos(a).toFloat(), c.y + inner * sin(a).toFloat()),
            end = Offset(c.x + r * 0.96f * cos(a).toFloat(), c.y + r * 0.96f * sin(a).toFloat()),
            strokeWidth = size.width * 0.02f,
        )
    }
    fun hand(angleDeg: Float, lengthScale: Float, stroke: Float) {
        val a = (angleDeg - 90f) * PI / 180f
        drawLine(
            Color.Black,
            start = c,
            end = Offset(c.x + r * lengthScale * cos(a).toFloat(), c.y + r * lengthScale * sin(a).toFloat()),
            strokeWidth = size.width * stroke,
        )
    }
    hand(300f, 0.5f, 0.045f) // hour  ≈ 10
    hand(60f, 0.75f, 0.035f) // minute ≈ 2
    hand(190f, 0.8f, 0.02f) // second
    drawCircle(Color(0xFFFF9F0A), radius = r * 0.05f, center = c)
}

private fun DrawScope.drawNotes() {
    drawRect(Color.White)
    val header = size.height * 0.24f
    drawRect(Color(0xFFFFD60A), size = Size(size.width, header))
    drawRect(Color(0xFFE5E5EA), topLeft = Offset(0f, header), size = Size(size.width, size.height * 0.012f))
    val lines = 4
    val ly = header + size.height * 0.16f
    val gap = size.height * 0.14f
    drawLine(Color.Black, Offset(size.width * 0.18f, ly), Offset(size.width * 0.55f, ly), size.width * 0.05f)
    for (i in 1..lines) {
        drawLine(
            Color(0xFFC7C7CC),
            Offset(size.width * 0.16f, ly + i * gap),
            Offset(size.width * 0.84f, ly + i * gap),
            size.width * 0.035f,
        )
    }
}

private fun DrawScope.drawWeather() {
    drawRect(
        Brush.verticalGradient(listOf(Color(0xFF1E7AD4), Color(0xFF63B0F0))),
    )
    // Sun
    drawCircle(Color(0xFFFFD60A), radius = size.width * 0.16f, center = Offset(size.width * 0.36f, size.height * 0.34f))
    // Cloud
    val cloud = Path().apply {
        val x = size.width * 0.26f
        val y = size.height * 0.60f
        val w = size.width * 0.48f
        moveTo(x, y + w * 0.2f)
        cubicTo(x, y - w * 0.05f, x + w * 0.25f, y - w * 0.08f, x + w * 0.32f, y + w * 0.02f)
        cubicTo(x + w * 0.36f, y - w * 0.22f, x + w * 0.72f, y - w * 0.20f, x + w * 0.72f, y + w * 0.05f)
        cubicTo(x + w * 0.95f, y + w * 0.05f, x + w * 0.95f, y + w * 0.28f, x + w * 0.72f, y + w * 0.28f)
        lineTo(x + w * 0.18f, y + w * 0.28f)
        cubicTo(x, y + w * 0.28f, x, y + w * 0.2f, x, y + w * 0.2f)
        close()
    }
    drawPath(cloud, Color.White)
}

private fun DrawScope.drawPhotos() {
    drawRect(Color(0xFFF9F9F9))
    val colors = listOf(
        Color(0xFFFFCC00), Color(0xFFFF9500), Color(0xFFFF3B30),
        Color(0xFFFF2D55), Color(0xFFAF52DE), Color(0xFF5856D6),
        Color(0xFF34C759), Color(0xFF30B0C7),
    )
    val cx = center.x
    val cy = center.y
    val petalR = size.width * 0.20f
    val orbit = size.width * 0.20f
    colors.forEachIndexed { i, color ->
        val a = i * (2 * PI / colors.size) + PI / 8
        drawCircle(
            color.copy(alpha = 0.85f),
            radius = petalR,
            center = Offset(cx + orbit * cos(a).toFloat(), cy + orbit * sin(a).toFloat()),
        )
    }
    drawCircle(Color.White, radius = size.width * 0.10f, center = center)
}

private fun DrawScope.drawSettings() {
    drawRect(
        Brush.linearGradient(listOf(Color(0xFF8E8E93), Color(0xFF48484A))),
    )
    val cx = center.x
    val cy = center.y
    val rOuter = size.width * 0.30f
    val rInner = size.width * 0.22f
    // Teeth
    for (i in 0 until 8) {
        val a = i * (2 * PI / 8)
        val px = cx + (rInner + (rOuter - rInner) / 2f) * cos(a).toFloat()
        val py = cy + (rInner + (rOuter - rInner) / 2f) * sin(a).toFloat()
        drawRoundRect(
            Color.White,
            topLeft = Offset(px - size.width * 0.045f, py - size.width * 0.045f),
            size = Size(size.width * 0.09f, size.width * 0.09f),
            cornerRadius = CornerRadius(size.width * 0.02f),
        )
    }
    drawCircle(Color.White, radius = rInner * 1.06f, center = center)
    drawCircle(Color(0xFF636366), radius = rInner * 0.72f, center = center)
}
