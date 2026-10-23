package com.somkov.greek_b1.reading

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.somkov.greek_b1.PlaceholderScreen
import com.somkov.greek_b1.R

@Composable
fun ReadingScreen(onBack: () -> Unit) {
    PlaceholderScreen(stringResource(R.string.reading), onBack)
}
