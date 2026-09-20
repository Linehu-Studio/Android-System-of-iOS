package com.linehu.asi.core.designsystem

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.geometry.Size
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

/**
 * Apple "squircle" — the continuous-corner superellipse used for app icons.
 * The superellipse |x/a|^n + |y/b|^n = 1 with n ≈ 5 matches iOS icon masks.
 */
class SquircleShape(private val n: Float = 5f) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline = Outline.Generic(squirclePath(size, n))
}

fun squirclePath(size: Size, n: Float = 5f): Path {
    val a = size.width / 2f
    val b = size.height / 2f
    val e = 2f / n // exponent applied to |cos|/|sin|
    val steps = 128
    val path = Path()
    for (i in 0..steps) {
        val theta = (i.toDouble() / steps) * 2.0 * PI
        val c = cos(theta).toFloat()
        val s = sin(theta).toFloat()
        val x = a * abs(c).pow(e) * if (c < 0) -1f else 1f
        val y = b * abs(s).pow(e) * if (s < 0) -1f else 1f
        val point = Offset(x + a, y + b)
        if (i == 0) path.moveTo(point.x, point.y) else path.lineTo(point.x, point.y)
    }
    path.close()
    return path
}

/** Clip a composable to an iOS icon squircle. */
fun Modifier.squircle(n: Float = 5f): Modifier = clip(SquircleShape(n))

/** iOS folder background: very rounded, not a full squircle. */
fun Modifier.folderShape(): Modifier = clip(RoundedCornerShape(percent = 38))
