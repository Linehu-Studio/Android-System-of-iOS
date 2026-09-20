package com.linehu.asi.core.gesture

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.background
import androidx.compose.ui.unit.dp

/** The iOS home indicator pill. */
@Composable
fun HomeIndicator(
    modifier: Modifier = Modifier,
    color: Color = Color(0xE6FFFFFF),
) {
    Box(
        modifier
            .fillMaxWidth()
            .padding(bottom = GestureMetrics.indicatorBottomMargin),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier
                    .width(GestureMetrics.indicatorWidth)
                    .height(GestureMetrics.indicatorHeight)
                    .clip(RoundedCornerShape(3.dp))
                    .background(color),
            )
        }
    }
}
