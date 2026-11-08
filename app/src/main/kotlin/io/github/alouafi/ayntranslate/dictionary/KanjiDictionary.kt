package io.github.alouafi.ayntranslate.dictionary

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.InputStream

/** The offline kanji dictionary built by `scripts/build_kanji_data.py`. Loaded on first lookup. */
class KanjiDictionary(private val open: () -> InputStream) {

    private val mutex = Mutex()

    @Volatile
    private var entries: Map<String, Kanji>? = null

    suspend fun lookup(literal: String): Kanji? = load()[literal]

    private suspend fun load(): Map<String, Kanji> = entries ?: mutex.withLock {
        entries ?: withContext(Dispatchers.IO) {
            open().bufferedReader().useLines(::parseKanjiTsv)
        }.also { entries = it }
    }
}

/** Parses `literal, strokes, grade, freq, radical, on, kun, meanings_en, meanings_fr` rows. Lists use `|`. */
internal fun parseKanjiTsv(lines: Sequence<String>): Map<String, Kanji> =
    lines.filter { it.isNotEmpty() && !it.startsWith("#") }
        .mapNotNull(::parseKanjiRow)
        .associateBy { it.literal }

private fun parseKanjiRow(row: String): Kanji? {
    val fields = row.split('\t')
    if (fields.size < FIELD_COUNT || fields[0].isEmpty()) return null
    return Kanji(
        literal = fields[0],
        strokes = fields[1].toIntOrNull() ?: 0,
        grade = fields[2].toIntOrNull(),
        frequency = fields[3].toIntOrNull(),
        radical = fields[4].toIntOrNull(),
        onReadings = fields[5].toList(),
        kunReadings = fields[6].toList(),
        meaningsEn = fields[7].toList(),
        meaningsFr = fields[8].toList(),
    )
}

private fun String.toList(): List<String> = if (isEmpty()) emptyList() else split('|')

private const val FIELD_COUNT = 9
