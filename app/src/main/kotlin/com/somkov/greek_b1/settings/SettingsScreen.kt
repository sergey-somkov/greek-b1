package com.somkov.greek_b1.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.somkov.greek_b1.R
import com.somkov.greek_b1.Screen
import com.somkov.greek_b1.ScreenScaffold

/** The settings. No setting yet (the first one comes in story 5), only the diagnostics. */
@Composable
fun SettingsScreen(onOpen: (Screen) -> Unit, onBack: () -> Unit) {
    ScreenScaffold(stringResource(R.string.settings), onBack) { innerPadding ->
        Column(Modifier.padding(innerPadding)) {
            ListItem(
                headlineContent = { Text(stringResource(R.string.diagnostics)) },
                supportingContent = { Text(stringResource(R.string.diagnostics_summary)) },
                modifier = Modifier.clickable { onOpen(Screen.Diagnostics) },
            )
        }
    }
}
