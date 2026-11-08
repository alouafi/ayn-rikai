package io.github.alouafi.ayntranslate.ui.companion

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.alouafi.ayntranslate.R
import io.github.alouafi.ayntranslate.dictionary.Kanji
import io.github.alouafi.ayntranslate.dictionary.Romaji
import io.github.alouafi.ayntranslate.dictionary.containsKanji
import io.github.alouafi.ayntranslate.settings.Settings
import io.github.alouafi.ayntranslate.ui.theme.JapaneseText

/** A dictionary page for a word or a kanji. [depth] is the number of pages open. */
@Composable
fun DetailPanel(
    detail: Detail,
    settings: Settings,
    depth: Int,
    onBack: () -> Unit,
    onClose: () -> Unit,
    onKanji: (String) -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 4.dp)) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
            }
            Spacer(Modifier.weight(1f))
            if (depth > 1) {
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = stringResource(R.string.action_close))
                }
            }
        }
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
        ) {
            when (detail) {
                is Detail.Word -> WordPage(detail, settings.targetLanguage, onKanji)
                is Detail.Character -> KanjiPage(detail.entry, settings.targetLanguage)
            }
        }
    }
}

@Composable
private fun WordPage(word: Detail.Word, language: String, onKanji: (String) -> Unit) {
    val token = word.token
    Column {
        if (containsKanji(token.surface) && token.reading.isNotEmpty()) {
            Text(token.reading, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        }
        Text(token.surface, style = JapaneseText.headword)
        Text(
            Romaji.fromKana(token.reading.ifEmpty { token.surface }),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    if (token.pos.isNotEmpty()) {
        Label(partOfSpeech(token.pos))
    }
    if (token.meaning.isNotEmpty()) {
        Text(token.meaning, style = MaterialTheme.typography.titleLarge)
    }
    if (token.base.isNotEmpty()) {
        Text(
            stringResource(R.string.word_dictionary_form, token.base),
            style = MaterialTheme.typography.bodyLarge,
        )
    }
    if (word.kanji.isNotEmpty()) {
        Text(
            stringResource(R.string.word_kanji),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp),
        )
        word.kanji.forEach { entry -> KanjiRow(entry, language, onClick = { onKanji(entry.literal) }) }
    }
}

@Composable
private fun KanjiRow(entry: KanjiEntry, language: String, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(12.dp)) {
            Text(entry.literal, style = JapaneseText.headword)
            Spacer(Modifier.width(16.dp))
            val kanji = entry.kanji
            Column {
                Text(
                    kanji?.meanings(language)?.joinToString(", ") ?: stringResource(R.string.kanji_not_found),
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (kanji != null) {
                    Text(
                        (kanji.onReadings + kanji.kunReadings.map(::formatKun)).joinToString("、"),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun KanjiPage(entry: KanjiEntry, language: String) {
    val kanji = entry.kanji
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(entry.literal, style = JapaneseText.kanji)
        Spacer(Modifier.width(16.dp))
        if (kanji != null) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                facts(kanji).forEach { fact ->
                    Text(fact, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
    if (kanji == null) {
        Text(stringResource(R.string.kanji_not_found), style = MaterialTheme.typography.bodyLarge)
        return
    }
    Text(kanji.meanings(language).joinToString(", "), style = MaterialTheme.typography.titleLarge)
    Readings(R.string.kanji_on, kanji.onReadings)
    Readings(R.string.kanji_kun, kanji.kunReadings.map(::formatKun))
}

@Composable
private fun facts(kanji: Kanji): List<String> = buildList {
    add(stringResource(R.string.kanji_strokes, kanji.strokes))
    when (val grade = kanji.grade) {
        null -> Unit
        in 1..6 -> add(stringResource(R.string.kanji_grade_school, grade))
        8 -> add(stringResource(R.string.kanji_grade_joyo))
        else -> add(stringResource(R.string.kanji_grade_names))
    }
    val radical = kanji.radical
    val symbol = kanji.radicalSymbol
    if (radical != null && symbol != null) add(stringResource(R.string.kanji_radical, symbol, radical))
    kanji.frequency?.let { add(stringResource(R.string.kanji_frequency, it)) }
}

@Composable
private fun Readings(@StringRes label: Int, readings: List<String>) {
    if (readings.isEmpty()) return
    Row {
        Text(
            stringResource(label),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.width(48.dp).padding(top = 4.dp),
        )
        Text(readings.joinToString("、"), style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun Label(text: String) {
    Surface(color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.16f), shape = MaterialTheme.shapes.small) {
        Text(
            text,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}

@Composable
private fun partOfSpeech(pos: String): String {
    val label = when (pos) {
        "noun" -> R.string.pos_noun
        "verb" -> R.string.pos_verb
        "adjective" -> R.string.pos_adjective
        "adverb" -> R.string.pos_adverb
        "particle" -> R.string.pos_particle
        "auxiliary" -> R.string.pos_auxiliary
        "pronoun" -> R.string.pos_pronoun
        "conjunction" -> R.string.pos_conjunction
        "interjection" -> R.string.pos_interjection
        "counter" -> R.string.pos_counter
        "name" -> R.string.pos_name
        "expression" -> R.string.pos_expression
        "punctuation" -> R.string.pos_punctuation
        "other" -> R.string.pos_other
        else -> return pos
    }
    return stringResource(label)
}

/** KANJIDIC2 writes okurigana after a dot: `かた.る` reads as かた（る）. */
internal fun formatKun(reading: String): String {
    val stem = reading.substringBefore('.')
    val okurigana = reading.substringAfter('.', missingDelimiterValue = "")
    return if (okurigana.isEmpty()) stem else "$stem（$okurigana）"
}
