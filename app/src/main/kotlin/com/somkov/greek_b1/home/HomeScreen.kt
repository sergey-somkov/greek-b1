package com.somkov.greek_b1.home

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.somkov.greek_b1.R
import com.somkov.greek_b1.Screen
import com.somkov.greek_b1.ScreenScaffold

private class Tile(val screen: Screen, @StringRes val title: Int)

// The four parts in the order of the exam, then progress and settings.
private val tiles = listOf(
    Tile(Screen.Reading, R.string.reading),
    Tile(Screen.Listening, R.string.listening),
    Tile(Screen.Writing, R.string.writing),
    Tile(Screen.Speaking, R.string.speaking),
    Tile(Screen.Progress, R.string.progress),
    Tile(Screen.Settings, R.string.settings),
)

/** The start screen: the countdown, and a tile for each of the other screens. */
@Composable
fun HomeScreen(onOpen: (Screen) -> Unit) {
    ScreenScaffold(stringResource(R.string.app_name)) { innerPadding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                CountdownCard(Modifier.fillMaxWidth())
            }
            items(tiles) { tile ->
                Card(onClick = { onOpen(tile.screen) }, modifier = Modifier.fillMaxWidth().height(96.dp)) {
                    Text(
                        stringResource(tile.title),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }
        }
    }
}
