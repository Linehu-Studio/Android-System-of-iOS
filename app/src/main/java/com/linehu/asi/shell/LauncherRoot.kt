package com.linehu.asi.shell

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.linehu.asi.core.animation.IosSprings
import com.linehu.asi.core.designsystem.BackdropImages
import com.linehu.asi.core.designsystem.FrostedPanel
import com.linehu.asi.core.designsystem.IosStatusBar
import com.linehu.asi.core.designsystem.LocalBackdrop
import com.linehu.asi.core.designsystem.rememberStatusBarState
import com.linehu.asi.core.gesture.GestureMetrics
import com.linehu.asi.core.gesture.HomeIndicator
import com.linehu.asi.core.gesture.edgePull
import com.linehu.asi.feature.assistivetouch.AssistiveTouchOverlay
import com.linehu.asi.feature.controlcenter.ControlCenterScreen
import com.linehu.asi.feature.dynamicisland.DynamicIsland
import com.linehu.asi.feature.lockscreen.LockScreenScene
import com.linehu.asi.feature.notificationcenter.NotificationCenterScreen
import com.linehu.asi.feature.recents.AppSwitcherScreen
import com.linehu.asi.feature.springboard.SpringboardScreen
import com.linehu.asi.feature.springboard.SpringboardViewModel
import com.linehu.asi.model.AppId
import com.linehu.asi.util.asiContainer
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * The shell: wallpaper, scene switcher, both "centers", dynamic island,
 * home-indicator gesture zone and AssistiveTouch. Everything on top of
 * everything, exactly like iOS.
 */
@Composable
fun LauncherRoot(
    shellViewModel: LauncherViewModel,
    springboardViewModel: SpringboardViewModel,
    backdrop: BackdropImages,
    modifier: Modifier = Modifier,
) {
    val shellState by shellViewModel.state.collectAsStateWithLifecycle()
    val sbState by springboardViewModel.state.collectAsStateWithLifecycle()
    val settings by springboardViewModel.settings.collectAsStateWithLifecycle()
    val container = LocalContext.current.asiContainer()
    val scope = rememberCoroutineScope()
    val view = LocalView.current

    val timerState by container.sharedTimer.state.collectAsStateWithLifecycle()
    val statusBar = rememberStatusBarState()

    // Gesture-driven reveal values for the two centers (0f..1f).
    val ccReveal = remember { Animatable(0f) }
    val ncReveal = remember { Animatable(0f) }
    LaunchedEffect(shellState.controlCenterOpen) {
        ccReveal.animateTo(if (shellState.controlCenterOpen) 1f else 0f, IosSprings.snappy())
    }
    LaunchedEffect(shellState.notificationCenterOpen) {
        ncReveal.animateTo(if (shellState.notificationCenterOpen) 1f else 0f, IosSprings.snappy())
    }

    // Built-in app open requests bubble from the springboard into the shell.
    LaunchedEffect(Unit) {
        springboardViewModel.builtinOpenEvents.collect { shellViewModel.openBuiltin(it) }
    }
    // Auto-dismiss the island "opening" flash.
    LaunchedEffect(shellState.islandFlashLabel) {
        if (shellState.islandFlashLabel != null) {
            delay(1000)
            shellViewModel.clearIslandFlash()
        }
    }

    BackHandler(enabled = true) {
        when {
            shellState.controlCenterOpen -> shellViewModel.setControlCenter(false)
            shellState.notificationCenterOpen -> shellViewModel.setNotificationCenter(false)
            shellState.jiggleMode -> shellViewModel.setJiggleMode(false)
            shellState.scene != Scene.Home -> shellViewModel.goHome()
        }
    }

    val pullEnabled = shellState.scene != Scene.LockScreen &&
        !shellState.controlCenterOpen && !shellState.notificationCenterOpen

    CompositionLocalProvider(LocalBackdrop provides backdrop) {
        Box(modifier.fillMaxSize().background(Color.Black)) {
            backdrop.regular?.let {
                Image(
                    bitmap = it,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            }

            // ---- Scenes ----
            AnimatedContent(
                targetState = shellState.scene,
                modifier = Modifier.fillMaxSize(),
                transitionSpec = {
                    // iOS "app opens from the icon" zoom-in feel.
                    (fadeIn(tween(220)) + scaleIn(initialScale = 0.86f, animationSpec = tween(220))) togetherWith
                        fadeOut(tween(160))
                },
                label = "scene",
            ) { scene ->
                when (scene) {
                    Scene.LockScreen -> LockScreenScene(
                        onUnlock = {
                            container.haptics.success()
                            shellViewModel.unlock()
                        },
                    )
                    Scene.Home -> SpringboardScreen(
                        pages = sbState.layout.pages,
                        dock = sbState.layout.dock,
                        apps = sbState.appsByKey.values.toList(),
                        iconLoader = { key, size ->
                            sbState.appsByKey[key]?.let { springboardViewModel.iconCache.iconFor(it, size) }
                        },
                        labelOf = springboardViewModel::labelOf,
                        onOpen = { entry ->
                            container.haptics.tap()
                            springboardViewModel.open(entry) { shellViewModel.flashIsland(it) }
                        },
                        onLongPress = { shellViewModel.setJiggleMode(true) },
                        jiggle = shellState.jiggleMode,
                        onJiggleDone = { shellViewModel.setJiggleMode(false) },
                        onMove = { page, from, to ->
                            springboardViewModel.moveEntry(
                                SpringboardViewModel.Slot(page, from),
                                SpringboardViewModel.Slot(page, to),
                            )
                        },
                        onMerge = { page, from, onto ->
                            springboardViewModel.mergeIntoFolder(
                                SpringboardViewModel.Slot(page, from),
                                SpringboardViewModel.Slot(page, onto),
                            )
                        },
                        onRemove = { page, index -> springboardViewModel.removeAt(page, index) },
                        onRenameFolder = { id, title -> springboardViewModel.renameFolder(id, title) },
                    )
                    is Scene.BuiltinApp -> BuiltinAppScene(scene.appId, container)
                    Scene.AppSwitcher -> AppSwitcherScreen(
                        repository = container.recentsRepository,
                        iconCache = container.iconCache,
                        usageStatsEnabled = settings.usageStatsEnabled,
                        onOpenApp = { app ->
                            container.appLauncher.launch(app, settings.freeformMode) {
                                shellViewModel.flashIsland(it)
                            }
                        },
                    )
                }
            }

            // ---- Control Center overlay ----
            if (ccReveal.value > 0.005f) {
                OverlayPanel(
                    reveal = ccReveal.value,
                    onDismiss = {
                        shellViewModel.setControlCenter(false)
                        scope.launch { ccReveal.animateTo(0f, IosSprings.snappy()) }
                    },
                ) {
                    ControlCenterScreen(
                        torch = container.torchController,
                        brightness = container.brightnessController,
                        volume = container.volumeController,
                        onOpenBuiltin = { shellViewModel.openBuiltin(it) },
                        onLock = {
                            shellViewModel.setControlCenter(false)
                            shellViewModel.lock()
                        },
                    )
                }
            }

            // ---- Notification Center overlay ----
            if (ncReveal.value > 0.005f) {
                OverlayPanel(
                    reveal = ncReveal.value,
                    onDismiss = {
                        shellViewModel.setNotificationCenter(false)
                        scope.launch { ncReveal.animateTo(0f, IosSprings.snappy()) }
                    },
                ) {
                    NotificationCenterScreen(
                        repository = container.notificationRepository,
                        onClearAll = { container.notificationRepository.clearAll() },
                    )
                }
            }

            // ---- Top edge pull strips (left → notifications, right → control) ----
            if (pullEnabled) {
                Box(
                    Modifier
                        .align(Alignment.TopStart)
                        .fillMaxWidth(0.55f)
                        .height(120.dp)
                        .edgePull(
                            onPull = { f -> scope.launch { ncReveal.snapTo(f.coerceIn(0f, 1f)) } },
                            onRelease = { f, v ->
                                val open = f > 0.35f || v > 1500f
                                shellViewModel.setNotificationCenter(open)
                                scope.launch {
                                    ncReveal.animateTo(if (open) 1f else 0f, IosSprings.snappy())
                                }
                            },
                        ),
                )
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .fillMaxWidth(0.45f)
                        .height(120.dp)
                        .edgePull(
                            onPull = { f -> scope.launch { ccReveal.snapTo(f.coerceIn(0f, 1f)) } },
                            onRelease = { f, v ->
                                val open = f > 0.35f || v > 1500f
                                shellViewModel.setControlCenter(open)
                                scope.launch {
                                    ccReveal.animateTo(if (open) 1f else 0f, IosSprings.snappy())
                                }
                            },
                        ),
                )
            }

            // ---- Status bar ----
            if (shellState.scene !is Scene.BuiltinApp) {
                IosStatusBar(
                    Modifier.align(Alignment.TopCenter),
                    contentColor = Color.White,
                    state = statusBar,
                )
            }

            // ---- Dynamic Island ----
            val cutoutWidth = remember {
                view.rootWindowInsets?.displayCutout?.boundingRects
                    ?.maxOfOrNull { it.width() }?.toFloat()
            }
            Box(Modifier.align(Alignment.TopCenter), contentAlignment = Alignment.TopCenter) {
                DynamicIsland(
                    flashLabel = shellState.islandFlashLabel,
                    timerState = timerState,
                    charging = statusBar.charging,
                    batteryPercent = statusBar.batteryPercent,
                    cutoutWidthPx = cutoutWidth,
                    onTap = {
                        when {
                            timerState.running -> shellViewModel.openBuiltin(AppId.CLOCK)
                            shellState.islandFlashLabel != null -> shellViewModel.clearIslandFlash()
                        }
                    },
                )
            }

            // ---- Home gesture zone + indicator ----
            HomeGestureZone(
                enabled = shellState.scene != Scene.LockScreen,
                onHome = {
                    shellViewModel.goHome()
                    scope.launch {
                        ccReveal.animateTo(0f, IosSprings.snappy())
                        ncReveal.animateTo(0f, IosSprings.snappy())
                    }
                },
                onSwitcher = { shellViewModel.openSwitcher() },
                modifier = Modifier.align(Alignment.BottomCenter),
            )
            if (shellState.scene != Scene.LockScreen) {
                HomeIndicator(Modifier.align(Alignment.BottomCenter))
            }

            // ---- AssistiveTouch ----
            if (shellState.assistiveTouchEnabled && shellState.scene != Scene.LockScreen) {
                AssistiveTouchOverlay(
                    onHome = { shellViewModel.goHome() },
                    onSwitcher = { shellViewModel.openSwitcher() },
                    onControlCenter = { shellViewModel.setControlCenter(true) },
                    onNotificationCenter = { shellViewModel.setNotificationCenter(true) },
                    onLock = { shellViewModel.lock() },
                    onSettings = { shellViewModel.openBuiltin(AppId.SETTINGS) },
                )
            }
        }
    }
}

/** Fullscreen overlay that slides down with the gesture and swipes up away. */
@Composable
private fun OverlayPanel(
    reveal: Float,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit,
) {
    Box(
        Modifier
            .fillMaxSize()
            .graphicsLayer {
                translationY = -(1f - reveal) * 0.55f * size.height
                alpha = 0.4f + 0.6f * reveal
            }
            .pointerInput(Unit) {
                detectVerticalDragGestures { change, dragAmount ->
                    change.consume()
                    if (dragAmount < -60) onDismiss()
                }
            },
    ) {
        FrostedPanel(Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxSize().pointerInput(Unit) {
                // Swallow vertical drags on the panel itself.
                detectVerticalDragGestures { change, _ -> change.consume() }
            }) {
                content()
            }
        }
    }
}

/**
 * Bottom swipe-up zone: quick flick (or any drag past threshold) goes home;
 * drag past the switcher threshold opens the app switcher.
 */
@Composable
private fun HomeGestureZone(
    enabled: Boolean,
    onHome: () -> Unit,
    onSwitcher: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!enabled) {
        Box(modifier.fillMaxWidth().height(GestureMetrics.gestureZoneHeight))
        return
    }
    Box(
        modifier
            .fillMaxWidth()
            .height(GestureMetrics.gestureZoneHeight)
            .pointerInput(Unit) {
                var totalDrag = 0f
                var switcherArmed = false
                detectVerticalDragGestures(
                    onDragStart = {
                        totalDrag = 0f
                        switcherArmed = false
                    },
                    onDragEnd = {
                        if (!switcherArmed && totalDrag < -40f) onHome()
                    },
                    onDragCancel = {},
                    onVerticalDrag = { change, dragAmount ->
                        totalDrag += dragAmount
                        if (totalDrag < -GestureMetrics.switcherThresholdPx && !switcherArmed) {
                            switcherArmed = true
                            onSwitcher()
                        }
                    },
                )
            },
    )
}

@Composable
private fun BuiltinAppScene(appId: AppId, container: com.linehu.asi.di.AppContainer) {
    when (appId) {
        AppId.CALCULATOR -> com.linehu.asi.feature.calculator.CalculatorScreen()
        AppId.CLOCK -> com.linehu.asi.feature.clock.ClockScreen(container.sharedTimer)
        AppId.NOTES -> com.linehu.asi.feature.notes.NotesScreen(container.notesRepository)
        AppId.WEATHER -> com.linehu.asi.feature.weather.WeatherScreen(container.weatherSource)
        AppId.PHOTOS -> com.linehu.asi.feature.photos.PhotosScreen()
        AppId.SETTINGS -> com.linehu.asi.feature.settings.SettingsScreen(container.settingsRepository)
    }
}
