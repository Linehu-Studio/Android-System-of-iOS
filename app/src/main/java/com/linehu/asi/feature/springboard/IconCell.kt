package com.linehu.asi.feature.springboard

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.linehu.asi.core.designsystem.IosText
import com.linehu.asi.core.designsystem.SquircleShape
import com.linehu.asi.model.GridEntry

/**
 * One icon + label cell. Supports jiggle mode (wobble + minus badge),
 * drag visuals (lift/scale) and async icon loading.
 */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun IconCell(
    entry: GridEntry,
    label: String,
    iconLoader: (suspend (String, Int) -> ImageBitmap?)?,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    iconShape: Shape = SquircleShape(),
    jiggle: Boolean = false,
    showMinus: Boolean = false,
    onMinus: () -> Unit = {},
    dragging: Boolean = false,
    dragOffsetX: Float = 0f,
    dragOffsetY: Float = 0f,
) {
    // Wobble rotation while in jiggle mode; each cell gets a phase offset so
    // the grid doesn't wobble in unison (iOS does random phase).
    val wobble = if (jiggle && !dragging) {
        val transition = rememberInfiniteTransition(label = "jiggle")
        val phase = remember { (0..6).random() * 80 }
        transition.animateFloat(
            initialValue = -2f,
            targetValue = 2f,
            animationSpec = infiniteRepeatable(
                animation = tween(360, delayMillis = phase),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "angle",
        ).value
    } else 0f

    Column(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                rotationZ = wobble
                translationX = dragOffsetX
                translationY = dragOffsetY
                scaleX = if (dragging) 1.18f else 1f
                scaleY = if (dragging) 1.18f else 1f
                shadowElevation = if (dragging) 24f else 0f
            }
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.fillMaxWidth().aspectRatio(1f), contentAlignment = Alignment.Center) {
            when (entry) {
                is GridEntry.AppEntry -> {
                    var bitmap by remember(entry.key) { mutableStateOf<ImageBitmap?>(null) }
                    LaunchedEffect(entry.key) {
                        bitmap = iconLoader?.invoke(entry.key, 256)
                    }
                    bitmap?.let { bmp ->
                        Image(
                            bitmap = bmp,
                            contentDescription = label,
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)
                                .clip(iconShape),
                        )
                    } ?: Box(
                        Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .clip(iconShape)
                            .background(Color(0x33FFFFFF)),
                    )
                }
                is GridEntry.BuiltinEntry -> BuiltinAppIcon(
                    entry.appId,
                    Modifier.fillMaxWidth().aspectRatio(1f),
                )
                is GridEntry.FolderEntry -> FolderIcon(entry, Modifier.fillMaxWidth().aspectRatio(1f))
            }

            // "Remove from home" badge in jiggle mode.
            if (jiggle && showMinus) {
                Box(
                    Modifier
                        .align(Alignment.TopStart)
                        .offset(x = (-6).dp, y = (-6).dp)
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(Color(0xE6C7C7CC))
                        .clickable(onClick = onMinus),
                    contentAlignment = Alignment.Center,
                ) {
                    IosText("−", fontSize = 15.sp, color = Color.Black, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                }
            }
        }
        if (label.isNotBlank()) {
            Spacer(Modifier.height(4.dp))
            IosText(
                text = label,
                fontSize = 12.sp,
                color = Color.White,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
