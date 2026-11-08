package io.github.alouafi.ayntranslate.ui.theme

import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp

/** Vermilion on true black: easy on AMOLED handheld screens next to a game. */
private val colors = darkColorScheme(
    primary = Color(0xFFFF7A59),
    onPrimary = Color(0xFF2B0A00),
    primaryContainer = Color(0xFF5C1A06),
    onPrimaryContainer = Color(0xFFFFDBD0),
    secondary = Color(0xFFB8C4FF),
    onSecondary = Color(0xFF0F1B4D),
    background = Color.Black,
    onBackground = Color(0xFFECE6E4),
    surface = Color.Black,
    onSurface = Color(0xFFECE6E4),
    surfaceVariant = Color(0xFF2A2523),
    onSurfaceVariant = Color(0xFFC9BEBA),
    surfaceContainerLowest = Color.Black,
    surfaceContainerLow = Color(0xFF0E0C0B),
    surfaceContainer = Color(0xFF151211),
    surfaceContainerHigh = Color(0xFF1E1A19),
    surfaceContainerHighest = Color(0xFF292423),
    outline = Color(0xFF8C807C),
    outlineVariant = Color(0xFF3D3633),
)

object JapaneseText {
    val line = TextStyle(fontSize = 24.sp, lineHeight = 30.sp)
    val furigana = TextStyle(fontSize = 11.sp, lineHeight = 13.sp)
    val headword = TextStyle(fontSize = 40.sp, lineHeight = 48.sp)
    val kanji = TextStyle(fontSize = 88.sp, lineHeight = 96.sp)
}

@Composable
fun AynTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = colors) {
        // Screens without a Scaffold or Surface would otherwise draw black text on the black window.
        CompositionLocalProvider(LocalContentColor provides colors.onBackground, content = content)
    }
}
