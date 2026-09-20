package com.linehu.asi.di

import android.content.Context
import com.linehu.asi.data.AppDatabase
import com.linehu.asi.data.apps.AppRepository
import com.linehu.asi.data.apps.IconCache
import com.linehu.asi.data.layout.LayoutRepository
import com.linehu.asi.data.notifications.NotificationRepository
import com.linehu.asi.data.notes.NotesRepository
import com.linehu.asi.data.recents.RecentsRepository
import com.linehu.asi.data.settings.SettingsRepository
import com.linehu.asi.data.timer.SharedTimer
import com.linehu.asi.data.weather.MockWeatherSource
import com.linehu.asi.data.weather.OpenMeteoSource
import com.linehu.asi.data.weather.WeatherDataSource
import com.linehu.asi.system.AppLauncher
import com.linehu.asi.system.BrightnessController
import com.linehu.asi.system.FreeformManager
import com.linehu.asi.system.TorchController
import com.linehu.asi.system.VolumeController

/**
 * Hand-rolled DI container. Created once in [com.linehu.asi.AsiApplication],
 * reachable via `LocalContext.current.asiContainer()`. No framework — the
 * dependency graph is small and constructor-wired.
 */
class AppContainer(private val appContext: Context) {

    val haptics by lazy { com.linehu.asi.core.designsystem.Haptics(appContext) }

    val database by lazy { AppDatabase.get(appContext) }

    val settingsRepository by lazy { SettingsRepository(appContext) }

    val appRepository by lazy { AppRepository(appContext) }

    val iconCache by lazy { IconCache(appContext) }

    val layoutRepository by lazy { LayoutRepository(database.layoutDao()) }

    val notesRepository by lazy { NotesRepository(database.noteDao()) }

    val freeformManager by lazy { FreeformManager(appContext) }

    val appLauncher by lazy {
        AppLauncher(appContext, database.launchHistoryDao(), freeformManager)
    }

    val sharedTimer by lazy { SharedTimer() }

    val notificationRepository by lazy { NotificationRepository() }

    val wallpaperRepository by lazy {
        com.linehu.asi.data.wallpaper.WallpaperRepository(appContext, settingsRepository)
    }

    val recentsRepository by lazy {
        RecentsRepository(appContext, database.launchHistoryDao(), appRepository)
    }

    val torchController by lazy { TorchController(appContext) }
    val brightnessController by lazy { BrightnessController(appContext) }
    val volumeController by lazy { VolumeController(appContext) }

    /** Mock by default so the weather app works fully offline. */
    val weatherSource: WeatherDataSource by lazy {
        System.getenv("ASI_ONLINE_WEATHER")?.let { OpenMeteoSource() } ?: MockWeatherSource()
    }
}
