package io.github.alouafi.aynrikai.ui.companion

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.alouafi.aynrikai.R
import io.github.alouafi.aynrikai.analysis.Token
import io.github.alouafi.aynrikai.dictionary.Inflection
import io.github.alouafi.aynrikai.dictionary.Romaji
import io.github.alouafi.aynrikai.dictionary.WordMatch
import io.github.alouafi.aynrikai.dictionary.containsKanji
import io.github.alouafi.aynrikai.settings.Settings
import io.github.alouafi.aynrikai.ui.theme.JapaneseText

/** What the tapped group means, under its line: its reading, the model's gloss, the dictionary words and the kanji. */
@Composable
fun LookupCard(lookup: Lookup, settings: Settings, onKanji: (KanjiEntry) -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Header(lookup, settings.showRomaji)
            lookup.token?.let { Gloss(it) }
            lookup.words.forEach { WordEntry(it) }
            lookup.kanji.forEach { entry -> KanjiRow(entry, settings.targetLanguage, onClick = { onKanji(entry) }) }
            if (lookup.token?.meaning.isNullOrEmpty() && lookup.words.isEmpty()) {
                Text(
                    stringResource(R.string.lookup_not_found),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun Header(lookup: Lookup, showRomaji: Boolean) {
    val reading = lookup.reading
    val romaji = (reading ?: lookup.text.takeUnless(::containsKanji))?.takeIf { showRomaji }?.let(Romaji::fromKana)
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(lookup.text, style = JapaneseText.lookup, modifier = Modifier.weight(1f))
            lookup.token?.pos?.takeIf { it.isNotEmpty() }?.let { Label(partOfSpeech(it)) }
        }
        if (reading != null || romaji != null) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                reading?.let {
                    Text(it, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                }
                romaji?.let {
                    Text(it, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

/** What the model says the group means in this line. */
@Composable
private fun Gloss(token: Token) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        if (token.meaning.isNotEmpty()) {
            Text(token.meaning, style = MaterialTheme.typography.titleMedium)
        }
        if (token.base.isNotEmpty() && token.base != token.surface) {
            Text(
                stringResource(R.string.word_dictionary_form, token.base),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** A JMdict word, with the conjugations undone to reach it. */
@Composable
private fun WordEntry(match: WordMatch) {
    val word = match.word
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(word.headword, style = JapaneseText.dictionaryWord, modifier = Modifier.alignByBaseline())
            if (word.spellings.isNotEmpty()) {
                Text(
                    word.readings.take(MAX_READINGS).joinToString("、"),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.alignByBaseline(),
                )
            }
        }
        if (match.inflections.isNotEmpty()) {
            Text(
                match.inflections.map { it.label() }.joinToString(" · "),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.secondary,
            )
        }
        val senses = word.senses.take(MAX_SENSES)
        senses.forEachIndexed { index, sense ->
            Text(
                if (senses.size > 1) "${index + 1}. $sense" else sense,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun KanjiRow(entry: KanjiEntry, language: String, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Text(entry.literal, style = JapaneseText.lookup)
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
                        (kanji.onReadings + kanji.kunReadings).joinToString("、"),
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

@Composable
private fun Inflection.label(): String = stringResource(
    when (this) {
        Inflection.POLITE -> R.string.inflection_polite
        Inflection.NEGATIVE -> R.string.inflection_negative
        Inflection.PAST -> R.string.inflection_past
        Inflection.TE -> R.string.inflection_te
        Inflection.CONDITIONAL -> R.string.inflection_conditional
        Inflection.TARA -> R.string.inflection_tara
        Inflection.TARI -> R.string.inflection_tari
        Inflection.VOLITIONAL -> R.string.inflection_volitional
        Inflection.IMPERATIVE -> R.string.inflection_imperative
        Inflection.POTENTIAL -> R.string.inflection_potential
        Inflection.PASSIVE -> R.string.inflection_passive
        Inflection.POTENTIAL_OR_PASSIVE -> R.string.inflection_potential_or_passive
        Inflection.CAUSATIVE -> R.string.inflection_causative
        Inflection.CAUSATIVE_PASSIVE -> R.string.inflection_causative_passive
        Inflection.WANT -> R.string.inflection_want
        Inflection.PROGRESSIVE -> R.string.inflection_progressive
        Inflection.COMPLETION -> R.string.inflection_completion
        Inflection.IN_ADVANCE -> R.string.inflection_in_advance
        Inflection.TRY -> R.string.inflection_try
        Inflection.REQUEST -> R.string.inflection_request
        Inflection.PERMISSION -> R.string.inflection_permission
        Inflection.MUST -> R.string.inflection_must
        Inflection.MUST_NOT -> R.string.inflection_must_not
        Inflection.WITHOUT -> R.string.inflection_without
        Inflection.WHILE -> R.string.inflection_while
        Inflection.COMMAND -> R.string.inflection_command
        Inflection.TOO_MUCH -> R.string.inflection_too_much
        Inflection.SEEMS -> R.string.inflection_seems
        Inflection.EASY -> R.string.inflection_easy
        Inflection.HARD -> R.string.inflection_hard
        Inflection.ADVERB -> R.string.inflection_adverb
        Inflection.NOUN -> R.string.inflection_noun
    },
)

private const val MAX_READINGS = 2
private const val MAX_SENSES = 3
