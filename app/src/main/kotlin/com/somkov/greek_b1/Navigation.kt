package com.somkov.greek_b1

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.somkov.greek_b1.diagnostics.DiagnosticsScreen
import com.somkov.greek_b1.home.HomeScreen
import com.somkov.greek_b1.listening.ListeningScreen
import com.somkov.greek_b1.progress.ProgressScreen
import com.somkov.greek_b1.reading.ReadingScreen
import com.somkov.greek_b1.settings.SettingsScreen
import com.somkov.greek_b1.speaking.SpeakingScreen
import com.somkov.greek_b1.writing.WritingScreen
import kotlinx.serialization.Serializable

/** The keys of the screens. The back stack is a list of them; the last one is shown. */
@Serializable
sealed interface Screen : NavKey {
    @Serializable data object Home : Screen
    @Serializable data object Reading : Screen
    @Serializable data object Listening : Screen
    @Serializable data object Writing : Screen
    @Serializable data object Speaking : Screen
    @Serializable data object Progress : Screen
    @Serializable data object Settings : Screen
    @Serializable data object Diagnostics : Screen
}

/**
 * The navigation of the app (Navigation 3): home opens the other screens, and the system back
 * gesture or the back arrow pops the last screen off the back stack. On home, back leaves the app.
 *
 * Navigation 3 is the current recommendation for Compose, and its back stack is a plain list that
 * the app owns. Home is a hub with tiles rather than a bottom bar: seven screens are too many for
 * a bar, and home becomes the daily plan in story 15.
 */
@Composable
fun AppNavigation() {
    val backStack = rememberNavBackStack(Screen.Home)
    // Home stays: a quick second tap on the back arrow during the transition must not empty the
    // stack, which NavDisplay does not accept.
    val onBack: () -> Unit = { if (backStack.size > 1) backStack.removeLastOrNull() }
    NavDisplay(
        backStack = backStack,
        onBack = onBack,
        // The default decorator (the saveable state of each entry), and a ViewModelStore for each
        // entry: a screen's ViewModel is cleared when the screen leaves the back stack.
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
        ),
        entryProvider = entryProvider {
            entry<Screen.Home> { HomeScreen(onOpen = { backStack.add(it) }) }
            entry<Screen.Reading> { ReadingScreen(onBack) }
            entry<Screen.Listening> { ListeningScreen(onBack) }
            entry<Screen.Writing> { WritingScreen(onBack) }
            entry<Screen.Speaking> { SpeakingScreen(onBack) }
            entry<Screen.Progress> { ProgressScreen(onBack) }
            entry<Screen.Settings> { SettingsScreen(onOpen = { backStack.add(it) }, onBack) }
            entry<Screen.Diagnostics> { DiagnosticsScreen(onBack) }
        },
    )
}
