package io.github.alouafi.aynrikai.dictionary

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.InputStream

/** A JMdict entry. Each sense is its glosses joined with "; ". */
data class Word(val spellings: List<String>, val readings: List<String>, val forms: Set<Form>, val senses: List<String>) {
    val headword: String get() = spellings.firstOrNull() ?: readings.first()
}

/** A word that the first [length] characters of a text are, once [inflections] are undone. */
data class WordMatch(val length: Int, val word: Word, val inflections: List<Inflection>)

/**
 * The offline word dictionary built by `scripts/build_word_data.py`, searched like rikaikun:
 * every prefix of the text, longest first, through the [Deinflector]. Loaded on first use.
 */
class WordDictionary(private val openWords: () -> InputStream, private val openIndex: () -> InputStream) {

    private val mutex = Mutex()

    @Volatile
    private var data: Data? = null

    /** Reads the dictionary ahead of the first lookup. */
    suspend fun load() {
        data()
    }

    /** The words that [text] starts with, longest first, then most common first. */
    suspend fun search(text: String): List<WordMatch> {
        val data = data()
        return withContext(Dispatchers.Default) { data.search(text) }
    }

    /** The range of the word that covers [offset] once [text] is split into words. */
    suspend fun wordAt(text: String, offset: Int): IntRange {
        require(offset in text.indices) { "Offset $offset is outside the text" }
        val data = data()
        return withContext(Dispatchers.Default) { data.split(text).first { offset in it } }
    }

    private suspend fun data(): Data = data ?: mutex.withLock {
        data ?: withContext(Dispatchers.IO) {
            Data(words = Lines(openWords().use { it.readBytes() }), index = Lines(openIndex().use { it.readBytes() }))
        }.also { data = it }
    }

    /** `words.tsv` rows, and `word_index.tsv` mapping each hiragana-folded spelling and reading to its rows. */
    private class Data(private val words: Lines, private val index: Lines) {

        fun search(text: String): List<WordMatch> {
            val found = LinkedHashMap<Int, WordMatch>()
            for (prefix in prefixes(text)) {
                for ((row, match) in matches(prefix)) found.putIfAbsent(row, match)
            }
            return found.entries
                .sortedWith(compareByDescending<Map.Entry<Int, WordMatch>> { it.value.length }.thenBy { it.key })
                .map { it.value }
        }

        /**
         * Splits [text] into the fewest words the dictionary knows, taking the longer first word on ties. Fewest words
         * beats longest-first, which would read 冒険者は行って as 冒険者, は行 (the は column), って.
         * A character that starts no word is a word of its own.
         */
        fun split(text: String): List<IntRange> {
            // fewest[i] is the fewest words that text from i splits into, and next[i] the length of the first one.
            val fewest = IntArray(text.length + 1)
            val next = IntArray(text.length)
            for (start in text.indices.reversed()) {
                fewest[start] = Int.MAX_VALUE
                for (length in wordLengths(text, start)) {
                    if (1 + fewest[start + length] < fewest[start]) {
                        fewest[start] = 1 + fewest[start + length]
                        next[start] = length
                    }
                }
            }
            return generateSequence(0) { it + next[it] }.takeWhile { it < text.length }.map { it until it + next[it] }.toList()
        }

        /** The lengths of the words at [start] of [text], longest first, or of its character when no word starts there. */
        private fun wordLengths(text: String, start: Int): List<Int> {
            val codePoint = text.codePointAt(start)
            val lengths = if (isJapanese(codePoint)) {
                prefixes(text.substring(start)).filter { matches(it).any() }.map { it.length }.toList()
            } else {
                emptyList()
            }
            return lengths.ifEmpty { listOf(Character.charCount(codePoint)) }
        }

        private fun prefixes(text: String): Sequence<String> {
            val window = text.take(MAX_WORD_LENGTH)
            return (window.length downTo 1).asSequence()
                .filterNot { window[it - 1].isHighSurrogate() }
                .map { window.substring(0, it) }
        }

        /** The rows that [prefix] can be, as written or deinflected, with the first deinflection that fits each. */
        private fun matches(prefix: String): Sequence<Pair<Int, WordMatch>> =
            Deinflector.deinflect(prefix).asSequence().flatMap { candidate ->
                rows(candidate.text).mapNotNull { row ->
                    val word = word(row)
                    if (candidate.accepts(word.forms)) row to WordMatch(prefix.length, word, candidate.inflections) else null
                }
            }

        private fun rows(key: String): Sequence<Int> {
            val line = index.find(toHiragana(key))
            if (line < 0) return emptySequence()
            return index[line].substringAfter('\t').splitToSequence(',').map(String::toInt)
        }

        private fun word(row: Int): Word {
            val (spellings, readings, classes, senses) = words[row].split('\t')
            return Word(
                spellings = spellings.toList(),
                readings = readings.toList(),
                forms = classes.toList().mapNotNull(Form::ofCode).toSet(),
                senses = senses.toList(),
            )
        }

        private fun String.toList(): List<String> = if (isEmpty()) emptyList() else split('|')
    }

    private companion object {
        /** Longer than any JMdict entry worth matching, so a lookup reads a bounded window. */
        const val MAX_WORD_LENGTH = 24
    }
}
