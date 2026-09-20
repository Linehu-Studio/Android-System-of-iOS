package com.linehu.asi

import android.app.Application
import com.linehu.asi.di.AppContainer

class AsiApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
