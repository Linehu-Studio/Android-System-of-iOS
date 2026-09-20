package com.linehu.asi.util

import android.content.Context
import com.linehu.asi.AsiApplication
import com.linehu.asi.di.AppContainer

fun Context.asiContainer(): AppContainer =
    (applicationContext as AsiApplication).container
