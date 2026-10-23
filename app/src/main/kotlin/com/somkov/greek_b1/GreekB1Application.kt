package com.somkov.greek_b1

import android.app.Application

/** Creates the [AppContainer] once per process. Screens reach it through the application object. */
class GreekB1Application : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
