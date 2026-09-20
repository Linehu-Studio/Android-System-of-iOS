package com.linehu.asi.shell

import androidx.lifecycle.ViewModel
import com.linehu.asi.model.AppId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class ShellState(
    val scene: Scene = Scene.LockScreen,
    val jiggleMode: Boolean = false,
    /** Transient "正在打开 xxx" label for the Dynamic Island. */
    val islandFlashLabel: String? = null,
    val controlCenterOpen: Boolean = false,
    val notificationCenterOpen: Boolean = false,
    val assistiveTouchEnabled: Boolean = true,
)

/**
 * Owns which scene is on screen and what "the home button" means right now.
 * Springboard/lock-screen state lives in their own ViewModels; this one is
 * deliberately tiny and purely about shell orchestration.
 */
class LauncherViewModel : ViewModel() {

    private val _state = MutableStateFlow(ShellState())
    val state: StateFlow<ShellState> = _state.asStateFlow()

    fun unlock() = _state.update { it.copy(scene = Scene.Home) }

    fun openBuiltin(appId: AppId) = _state.update {
        it.copy(scene = Scene.BuiltinApp(appId), jiggleMode = false, controlCenterOpen = false, notificationCenterOpen = false)
    }

    fun openSwitcher() = _state.update {
        it.copy(scene = Scene.AppSwitcher, controlCenterOpen = false, notificationCenterOpen = false)
    }

    fun openHome() = goHome()

    fun lock() = _state.update {
        it.copy(scene = Scene.LockScreen, controlCenterOpen = false, notificationCenterOpen = false)
    }

    /** HOME intent / home-indicator swipe: back to the desktop, everything closed. */
    fun goHome() = _state.update {
        if (it.scene == Scene.LockScreen) it
        else it.copy(scene = Scene.Home, jiggleMode = false, controlCenterOpen = false, notificationCenterOpen = false)
    }

    fun setJiggleMode(enabled: Boolean) = _state.update { it.copy(jiggleMode = enabled) }

    fun setControlCenter(open: Boolean) = _state.update {
        it.copy(controlCenterOpen = open, notificationCenterOpen = if (open) false else it.notificationCenterOpen)
    }

    fun setNotificationCenter(open: Boolean) = _state.update {
        it.copy(notificationCenterOpen = open, controlCenterOpen = if (open) false else it.controlCenterOpen)
    }

    fun toggleAssistiveTouch() = _state.update { it.copy(assistiveTouchEnabled = !it.assistiveTouchEnabled) }

    fun flashIsland(label: String) = _state.update { it.copy(islandFlashLabel = label) }

    fun clearIslandFlash() = _state.update { it.copy(islandFlashLabel = null) }
}
