package com.somkov.greek_b1

import android.content.Context
import com.somkov.greek_b1.data.AppDatabase

/**
 * Manual dependency injection: the objects that live as long as the app (the database, the
 * clients of the platform services) are created here and passed to the screens' ViewModels.
 * There is one instance, held by [GreekB1Application]. If this class grows too large, Hilt
 * replaces it.
 */
class AppContainer(context: Context) {
    /** Built at the start of the app, but the file is opened on the first query. */
    val database: AppDatabase = AppDatabase.build(context)
}
