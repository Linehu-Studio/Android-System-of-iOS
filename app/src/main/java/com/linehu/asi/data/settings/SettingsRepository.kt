package com.linehu.asi.data.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "asi_settings")

enum class ThemeMode { SYSTEM, LIGHT, DARK }
enum class IconStyle { NATIVE, IOS }
enum class FreeformMode { AUTO, ON, OFF }

data class AsiSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val iconStyle: IconStyle = IconStyle.IOS,
    val freeformMode: FreeformMode = FreeformMode.AUTO,
    val lockTimeoutMinutes: Int = 0, // 0 = only lock on cold start
    val usageStatsEnabled: Boolean = false,
    val builtinIconsIosStyle: Boolean = true,
    /** 0 = system wallpaper, 1..N = built-in gradient. */
    val wallpaperId: Int = 0,
)

class SettingsRepository(private val context: Context) {

    private object Keys {
        val THEME = stringPreferencesKey("theme_mode")
        val ICON_STYLE = stringPreferencesKey("icon_style")
        val FREEFORM = stringPreferencesKey("freeform_mode")
        val LOCK_TIMEOUT = intPreferencesKey("lock_timeout_minutes")
        val USAGE_STATS = booleanPreferencesKey("usage_stats_enabled")
        val WALLPAPER = intPreferencesKey("wallpaper_id")
    }

    val settings: Flow<AsiSettings> = context.dataStore.data.map { p ->
        AsiSettings(
            themeMode = p[Keys.THEME]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: ThemeMode.SYSTEM,
            iconStyle = p[Keys.ICON_STYLE]?.let { runCatching { IconStyle.valueOf(it) }.getOrNull() } ?: IconStyle.IOS,
            freeformMode = p[Keys.FREEFORM]?.let { runCatching { FreeformMode.valueOf(it) }.getOrNull() } ?: FreeformMode.AUTO,
            lockTimeoutMinutes = p[Keys.LOCK_TIMEOUT] ?: 0,
            usageStatsEnabled = p[Keys.USAGE_STATS] ?: false,
            wallpaperId = p[Keys.WALLPAPER] ?: 0,
        )
    }

    suspend fun setThemeMode(mode: ThemeMode) = context.dataStore.edit { it[Keys.THEME] = mode.name }
    suspend fun setIconStyle(style: IconStyle) = context.dataStore.edit { it[Keys.ICON_STYLE] = style.name }
    suspend fun setFreeformMode(mode: FreeformMode) = context.dataStore.edit { it[Keys.FREEFORM] = mode.name }
    suspend fun setLockTimeout(minutes: Int) = context.dataStore.edit { it[Keys.LOCK_TIMEOUT] = minutes }
    suspend fun setUsageStatsEnabled(enabled: Boolean) = context.dataStore.edit { it[Keys.USAGE_STATS] = enabled }
    suspend fun setWallpaperId(id: Int) = context.dataStore.edit { it[Keys.WALLPAPER] = id }
}
