package com.linehu.asi.core.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

/**
 * App-wide theme. Provides [LocalIosColors] for our own surfaces and keeps a
 * MaterialTheme in sync so occasional M3 widgets (Slider, Switch…) inherit
 * iOS-ish colors instead of Material defaults.
 */
@Composable
fun AsiTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val iosColors = if (darkTheme) DarkIosColors else LightIosColors
    val materialScheme = if (darkTheme) {
        darkColorScheme(
            primary = iosColors.blue,
            background = iosColors.background,
            surface = iosColors.surface,
        )
    } else {
        lightColorScheme(
            primary = iosColors.blue,
            background = iosColors.background,
            surface = iosColors.surface,
        )
    }
    CompositionLocalProvider(LocalIosColors provides iosColors) {
        MaterialTheme(colorScheme = materialScheme, content = content)
    }
}
