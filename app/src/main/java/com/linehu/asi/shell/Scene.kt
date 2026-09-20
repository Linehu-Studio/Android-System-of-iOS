package com.linehu.asi.shell

import com.linehu.asi.model.AppId

/**
 * The shell's scene graph. Not a navigation stack — iOS-like fullscreen
 * scenes the user gestures between; overlays (control center etc.) live in
 * [ShellState] as independent reveal values.
 */
sealed interface Scene {
    data object LockScreen : Scene
    data object Home : Scene
    data class BuiltinApp(val appId: AppId) : Scene
    data object AppSwitcher : Scene
}
