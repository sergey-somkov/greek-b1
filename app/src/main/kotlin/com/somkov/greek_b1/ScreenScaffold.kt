package com.somkov.greek_b1

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

/**
 * The frame of a screen: a top bar with the title, and the content below it. With [onBack], the
 * top bar has a back arrow that does the same as the system back gesture, since the user prefers
 * visible buttons; home has none.
 *
 * It is the frame of every screen, not a feature, so it stays in the root package; a shared UI
 * package can come when there is more to share.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreenScaffold(title: String, onBack: (() -> Unit)? = null, content: @Composable (PaddingValues) -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(painterResource(R.drawable.arrow_back), contentDescription = stringResource(R.string.back))
                        }
                    }
                },
            )
        },
        content = content,
    )
}

/** A screen that is not built yet: its title and a note. */
@Composable
fun PlaceholderScreen(title: String, onBack: () -> Unit) {
    ScreenScaffold(title, onBack) { innerPadding ->
        Text(
            stringResource(R.string.placeholder),
            modifier = Modifier.padding(innerPadding).padding(16.dp),
        )
    }
}
