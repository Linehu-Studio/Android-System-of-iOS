package com.linehu.asi

import android.content.Intent
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.linehu.asi.core.designsystem.AsiTheme
import com.linehu.asi.core.designsystem.BackdropImages
import com.linehu.asi.feature.springboard.SpringboardViewModel
import com.linehu.asi.shell.LauncherRoot
import com.linehu.asi.shell.LauncherViewModel
import com.linehu.asi.util.asiContainer

class MainActivity : ComponentActivity() {

    internal val shellViewModel = LauncherViewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        hideSystemBars()
        setContent {
            val container = asiContainer()
            val settings by container.settingsRepository.settings.collectAsStateWithLifecycle(
                initialValue = com.linehu.asi.data.settings.AsiSettings(),
            )
            val dark = when (settings.themeMode) {
                com.linehu.asi.data.settings.ThemeMode.SYSTEM -> androidx.compose.foundation.isSystemInDarkTheme()
                com.linehu.asi.data.settings.ThemeMode.LIGHT -> false
                com.linehu.asi.data.settings.ThemeMode.DARK -> true
            }
            AsiTheme(darkTheme = dark) {
                LauncherContent()
            }
        }
        // HOME intents arrive as onNewIntent after the first launch.
        if (intent?.isHomeIntent() == true && savedInstanceState != null) {
            shellViewModel.goHome()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.isHomeIntent()) {
            // System gesture nav "swipe home" lands here — same semantics as
            // our own home indicator.
            shellViewModel.goHome()
        }
    }

    private fun Intent?.isHomeIntent(): Boolean =
        this?.categories?.contains(Intent.CATEGORY_HOME) == true

    private fun hideSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) excludeGestureZone()
    }

    /** Keep the system from stealing our home-indicator swipe (API 29+). */
    private fun excludeGestureZone() {
        if (Build.VERSION.SDK_INT < 29) return
        val density = resources.displayMetrics.density
        val height = window.decorView.height
        val zonePx = (44 * density).toInt().coerceAtMost(height / 3)
        val rect = Rect(0, height - zonePx, window.decorView.width, height)
        runCatching {
            window.decorView.setSystemGestureExclusionRects(listOf(rect))
        }
    }
}

@androidx.compose.runtime.Composable
private fun LauncherContent() {
    val context = LocalContext.current
    val container = context.asiContainer()
    val backdrop by container.wallpaperRepository.backdropFlow.collectAsStateWithLifecycle(
        initialValue = BackdropImages(regular = null, blurred = null),
    )
    val springboardViewModel = viewModel {
        SpringboardViewModel(
            appContext = context.applicationContext,
            appRepository = container.appRepository,
            layoutRepository = container.layoutRepository,
            settingsRepository = container.settingsRepository,
            appLauncher = container.appLauncher,
            iconCache = container.iconCache,
        )
    }
    LauncherRoot(
        shellViewModel = (context as MainActivity).shellViewModel,
        springboardViewModel = springboardViewModel,
        backdrop = backdrop,
    )
}
