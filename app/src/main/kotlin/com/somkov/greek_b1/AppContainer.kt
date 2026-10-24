package com.somkov.greek_b1

import android.content.Context
import androidx.datastore.preferences.preferencesDataStoreFile
import com.somkov.greek_b1.content.ContentLoader
import com.somkov.greek_b1.data.AppDatabase
import com.somkov.greek_b1.session.PhoneClock
import com.somkov.greek_b1.session.SessionRecorder
import com.somkov.greek_b1.settings.Settings

/**
 * Manual dependency injection: the objects that live as long as the app (the database, the
 * settings, the clients of the platform services) are created here and passed to the screens'
 * ViewModels. There is one instance, held by [GreekB1Application]. If this class grows too
 * large, Hilt replaces it.
 */
class AppContainer(context: Context) {
    /** Built at the start of the app, but the file is opened on the first query. */
    val database: AppDatabase = AppDatabase.build(context)

    val contentLoader: ContentLoader = ContentLoader.fromAssets(database, context.assets)

    val sessionRecorder: SessionRecorder = SessionRecorder(database, PhoneClock)

    /** The file is read on the first use: `files/datastore/settings.preferences_pb`. */
    val settings: Settings = Settings.open(context.preferencesDataStoreFile("settings"))
}
