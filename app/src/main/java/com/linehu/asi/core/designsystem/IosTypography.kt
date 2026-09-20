package com.linehu.asi.core.designsystem

import androidx.compose.ui.text.font.FontFamily

/**
 * SF Pro cannot be bundled (Apple licensing), so we use the system sans with
 * SF-like weights. On most devices Roboto at SemiBold reads very close.
 */
object IosTypography {
    val sfProFamily: FontFamily = FontFamily.SansSerif
}
