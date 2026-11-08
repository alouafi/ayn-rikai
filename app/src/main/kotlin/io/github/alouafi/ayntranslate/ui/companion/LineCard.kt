package io.github.alouafi.ayntranslate.ui.companion

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import io.github.alouafi.ayntranslate.analysis.Line
import io.github.alouafi.ayntranslate.analysis.Token
import io.github.alouafi.ayntranslate.dictionary.Romaji
import io.github.alouafi.ayntranslate.dictionary.characters
import io.github.alouafi.ayntranslate.dictionary.containsKanji
import io.github.alouafi.ayntranslate.dictionary.isKanji
import io.github.alouafi.ayntranslate.settings.Settings
import io.github.alouafi.ayntranslate.ui.theme.JapaneseText

/** One line of game text: words with furigana, then the translation and a grammar note. */
@Composable
fun LineCard(line: Line, settings: Settings, dimmed: Boolean, onWord: (Token) -> Unit, onKanji: (String) -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth().alpha(if (dimmed) DIMMED_ALPHA else 1f),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            if (line.tokens.isNotEmpty()) {
                Words(line.tokens, settings.showFurigana, onWord)
            } else {
                Characters(line.japanese, onKanji)
            }
            if (settings.showRomaji) {
                Text(
                    romaji(line),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (line.translation.isNotEmpty()) {
                Text(line.translation, style = MaterialTheme.typography.bodyLarge)
            }
            if (line.grammar.isNotEmpty()) {
                Text(
                    line.grammar,
                    style = MaterialTheme.typography.bodyMedium,
                    fontStyle = FontStyle.Italic,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun Words(tokens: List<Token>, showFurigana: Boolean, onWord: (Token) -> Unit) {
    val highlight = MaterialTheme.colorScheme.surfaceVariant
    FlowRow(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        tokens.forEach { token ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.tappable(enabled = token.isWord, highlight) { onWord(token) },
            ) {
                if (showFurigana) {
                    // Every word keeps the furigana row so the line stays aligned.
                    Text(
                        if (containsKanji(token.surface)) token.reading else "",
                        style = JapaneseText.furigana,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                    )
                }
                Text(token.surface, style = JapaneseText.line)
            }
        }
    }
}

/** OCR-only lines have no word split, so each kanji is tappable on its own. */
@Composable
private fun Characters(text: String, onKanji: (String) -> Unit) {
    val highlight = MaterialTheme.colorScheme.surfaceVariant
    FlowRow(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        characters(text).forEach { character ->
            val kanji = isKanji(character.codePointAt(0))
            Text(
                character,
                style = JapaneseText.line,
                modifier = Modifier.tappable(enabled = kanji, highlight) { onKanji(character) },
            )
        }
    }
}

private fun Modifier.tappable(enabled: Boolean, highlight: Color, onClick: () -> Unit): Modifier {
    if (!enabled) return padding(horizontal = 1.dp)
    return clip(RoundedCornerShape(6.dp))
        .background(highlight)
        .clickable(onClick = onClick)
        .padding(horizontal = 3.dp, vertical = 1.dp)
}

private fun romaji(line: Line): String =
    if (line.tokens.isEmpty()) {
        Romaji.fromKana(line.japanese)
    } else {
        line.tokens.filter { it.isWord }.joinToString(" ") { Romaji.fromKana(it.reading.ifEmpty { it.surface }) }
    }

private const val DIMMED_ALPHA = 0.6f
