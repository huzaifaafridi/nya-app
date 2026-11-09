package com.nya.app

import android.app.Application
import com.nya.app.data.AppContainercal

class NyaApp : Application() {
    lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
