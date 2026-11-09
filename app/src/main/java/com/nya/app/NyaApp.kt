package com.nya.app

import android.app.Application
import com.nya.app.data.AppContainer

class NyaApp : Application() {
    lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()
        // before anything talks to Firebase AI Logic, which needs a valid App Check token
        installAppCheck()
        container = AppContainer(this)
    }
}
