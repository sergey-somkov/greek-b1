package com.somkov.greek_b1

import android.app.Application
import android.util.Log
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

private const val TAG = "GreekB1"

/** Creates the [AppContainer] once per process. Screens reach it through the application object. */
class GreekB1Application : Application() {
    lateinit var container: AppContainer
        private set

    /** The work of the app that outlives its screens; it ends with the process. */
    private val scope = MainScope()

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        loadContent()
    }

    /**
     * Loads the content of the APK if it is new, in the background, so the start is not delayed.
     * A screen that shows the content observes it with a Room flow, which updates when the load
     * commits. A failed load keeps the old content and is only logged: the next start tries
     * again.
     */
    private fun loadContent() {
        scope.launch {
            try {
                if (container.contentLoader.load()) Log.i(TAG, "content loaded")
            } catch (e: Exception) {
                Log.e(TAG, "content load failed, the old content is kept", e)
            }
        }
    }
}
