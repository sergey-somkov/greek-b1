package com.somkov.greek_b1.writing

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.somkov.greek_b1.PlaceholderScreen
import com.somkov.greek_b1.R

@Composable
fun WritingScreen(onBack: () -> Unit) {
    PlaceholderScreen(stringResource(R.string.writing), onBack)
}
