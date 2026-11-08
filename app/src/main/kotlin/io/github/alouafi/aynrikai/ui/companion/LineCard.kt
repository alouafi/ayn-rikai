package io.github.alouafi.aynrikai.ui.companion

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import io.github.alouafi.aynrikai.analysis.Line
import io.github.alouafi.aynrikai.dictionary.Romaji
import io.github.alouafi.aynrikai.dictionary.containsKanji
import io.github.alouafi.aynrikai.settings.Settings
import io.github.alouafi.aynrikai.ui.theme.JapaneseText

/**
 * One line of game text: the Japanese, where tapping a character looks up its group with [onTap] and the offset
 * of the character in the line's [Line.surface]; the [lookup] of the tapped group; then the translation and a grammar note.
 */
@Composable
fun LineCard(
    line: Line,
    settings: Settings,
    dimmed: Boolean,
    lookup: Lookup?,
    onTap: (offset: Int) -> Unit,
    onKanji: (KanjiEntry) -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth().alpha(if (dimmed) DIMMED_ALPHA else 1f),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            if (line.tokens.isNotEmpty()) {
                Words(line, settings.showFurigana, lookup?.range, onTap)
            } else {
                PlainText(line.japanese, lookup?.range, onTap)
            }
            if (lookup != null) {
                LookupCard(lookup, settings, onKanji)
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

/** Lines the model split into words: each word is a chip with its furigana. */
@Composable
private fun Words(line: Line, showFurigana: Boolean, selected: IntRange?, onTap: (Int) -> Unit) {
    val colors = MaterialTheme.colorScheme
    FlowRow(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        for ((token, range) in line.tokens.zip(line.tokenRanges())) {
            val highlight = if (range == selected) colors.primaryContainer else colors.surfaceVariant
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.tappable(enabled = token.isWord, highlight) { onTap(range.first) },
            ) {
                if (showFurigana) {
                    // Every word keeps the furigana row so the line stays aligned.
                    Text(
                        if (containsKanji(token.surface)) token.reading else "",
                        style = JapaneseText.furigana,
                        color = colors.primary,
                        maxLines = 1,
                    )
                }
                Text(token.surface, style = JapaneseText.line)
            }
        }
    }
}

/** Lines without a word split: any character can be tapped, and the dictionary finds the word around it. */
@Composable
private fun PlainText(text: String, selected: IntRange?, onTap: (Int) -> Unit) {
    val colors = MaterialTheme.colorScheme
    val styled = buildAnnotatedString {
        append(text)
        if (selected != null) {
            val style = SpanStyle(color = colors.onPrimaryContainer, background = colors.primaryContainer)
            addStyle(style, selected.first, selected.last + 1)
        }
    }
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
    val currentOnTap by rememberUpdatedState(onTap)
    Text(
        styled,
        style = JapaneseText.line,
        onTextLayout = { layout = it },
        modifier = Modifier.pointerInput(Unit) {
            detectTapGestures { position -> layout?.characterAt(position)?.let(currentOnTap) }
        },
    )
}

/** The index of the character drawn under [position], or null when the tap is beside the text. */
private fun TextLayoutResult.characterAt(position: Offset): Int? {
    // The nearest caret position follows the character when the tap is on its right half.
    val caret = getOffsetForPosition(position)
    return listOf(caret, caret - 1).firstOrNull {
        it in layoutInput.text.indices && getBoundingBox(it).contains(position)
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
