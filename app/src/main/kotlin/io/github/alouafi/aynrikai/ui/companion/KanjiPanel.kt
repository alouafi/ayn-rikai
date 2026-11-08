package io.github.alouafi.aynrikai.ui.companion

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.alouafi.aynrikai.R
import io.github.alouafi.aynrikai.dictionary.Kanji
import io.github.alouafi.aynrikai.ui.theme.JapaneseText

/** The KANJIDIC2 page of a kanji. */
@Composable
fun KanjiPanel(entry: KanjiEntry, language: String, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        IconButton(onClick = onBack, modifier = Modifier.padding(horizontal = 4.dp)) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
        }
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
        ) {
            KanjiPage(entry, language)
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
    Readings(R.string.kanji_kun, kanji.kunReadings)
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
