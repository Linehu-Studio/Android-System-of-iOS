package com.linehu.asi.core.designsystem

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * iOS 18 system palette. Values follow Apple's light/dark semantic colors
 * closely enough that screenshots read as "iOS".
 */
data class IosColors(
    val label: Color,
    val secondaryLabel: Color,
    val tertiaryLabel: Color,
    val quaternaryLabel: Color,
    val background: Color,
    val surface: Color,
    val surfaceElevated: Color,
    val surfaceBlurTint: Color,
    val separator: Color,
    val blue: Color,
    val green: Color,
    val red: Color,
    val orange: Color,
    val yellow: Color,
    val teal: Color,
    val indigo: Color,
    val purple: Color,
    val pink: Color,
    val fill: Color,
    val darkLabelOnLightBlur: Color,
)

val LightIosColors = IosColors(
    label = Color(0xFF000000),
    secondaryLabel = Color(0x993C3C43),
    tertiaryLabel = Color(0x4D3C3C43),
    quaternaryLabel = Color(0x2E3C3C43),
    background = Color(0xFFF2F2F7),
    surface = Color(0xFFFFFFFF),
    surfaceElevated = Color(0xFFFFFFFF),
    surfaceBlurTint = Color(0xD8F2F2F7),
    separator = Color(0x333C3C43),
    blue = Color(0xFF007AFF),
    green = Color(0xFF34C759),
    red = Color(0xFFFF3B30),
    orange = Color(0xFFFF9500),
    yellow = Color(0xFFFFCC00),
    teal = Color(0xFF30B0C7),
    indigo = Color(0xFF5856D6),
    purple = Color(0xFFAF52DE),
    pink = Color(0xFFFF2D55),
    fill = Color(0x1F3C3C43),
    darkLabelOnLightBlur = Color(0xFF000000),
)

val DarkIosColors = IosColors(
    label = Color(0xFFFFFFFF),
    secondaryLabel = Color(0x99EBEBF5),
    tertiaryLabel = Color(0x4DEBEBF5),
    quaternaryLabel = Color(0x2DEBEBF5),
    background = Color(0xFF000000),
    surface = Color(0xFF1C1C1E),
    surfaceElevated = Color(0xFF2C2C2E),
    surfaceBlurTint = Color(0xB31C1C1E),
    separator = Color(0x33EBEBF5),
    blue = Color(0xFF0A84FF),
    green = Color(0xFF30D158),
    red = Color(0xFFFF453A),
    orange = Color(0xFFFF9F0A),
    yellow = Color(0xFFFFD60A),
    teal = Color(0xFF40C8E0),
    indigo = Color(0xFF5E5CE6),
    purple = Color(0xFFBF5AF2),
    pink = Color(0xFFFF375F),
    fill = Color(0x1FEBEBF5),
    darkLabelOnLightBlur = Color(0xFFFFFFFF),
)

val LocalIosColors = staticCompositionLocalOf { DarkIosColors }
