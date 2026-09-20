package com.linehu.asi.core.designsystem

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale

/**
 * Holds the current wallpaper (regular + pre-blurred). Overlays (Control
 * Center, Spotlight, folders…) paint the blurred copy plus a scrim to get a
 * frosted-glass look on every API level — real-time backdrop blur only exists
 * on 31+, but a pre-blurred wallpaper under a translucent tint is visually
 * equivalent for fullscreen panels.
 */
class BackdropImages(
    val regular: ImageBitmap?,
    val blurred: ImageBitmap?,
)

val LocalBackdrop = staticCompositionLocalOf<BackdropImages?> { null }

/**
 * Frosted panel: blurred wallpaper + iOS vibrancy scrim + content.
 */
@Composable
fun FrostedPanel(
    modifier: Modifier = Modifier,
    scrim: Color = LocalIosColors.current.surfaceBlurTint,
    content: @Composable () -> Unit,
) {
    val backdrop = LocalBackdrop.current
    Box(modifier) {
        if (backdrop?.blurred != null) {
            Image(
                bitmap = backdrop.blurred,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }
        Box(Modifier.fillMaxSize().background(scrim))
        content()
    }
}
