package com.somkov.greek_b1.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

/**
 * The Material 3 theme of the app: the dynamic colors of the phone (from the wallpaper), light
 * or dark as the system is set, and the default typography and shapes: no palette of our own to
 * design and keep. minSdk is above 31, so dynamic colors are always available.
 */
@Composable
fun GreekB1Theme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val colorScheme = if (isSystemInDarkTheme()) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    MaterialTheme(colorScheme = colorScheme, content = content)
}
