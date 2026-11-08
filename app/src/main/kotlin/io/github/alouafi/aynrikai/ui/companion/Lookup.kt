package io.github.alouafi.aynrikai.ui.companion

import io.github.alouafi.aynrikai.analysis.Line
import io.github.alouafi.aynrikai.analysis.Token
import io.github.alouafi.aynrikai.dictionary.WordMatch

/**
 * The group of characters the player tapped in a line: a word with its endings, a compound or a grammar pattern.
 * [range] is where the group is in the line's [Line.surface].
 */
data class Lookup(
    val resultId: Long,
    val line: Int,
    val range: IntRange,
    val text: String,
    /** What the model said about the group, on lines it split into tokens. */
    val token: Token?,
    /** Dictionary words the group starts with, longest first. */
    val words: List<WordMatch>,
    /** The kanji in the group and in its dictionary form. */
    val kanji: List<KanjiEntry>,
) {
    /** The kana reading of the whole group when the model or an uninflected dictionary word gives it, and it adds something. */
    val reading: String?
        get() {
            val reading = token?.reading?.takeIf { it.isNotEmpty() }
                ?: words.firstOrNull { it.length == text.length && it.inflections.isEmpty() }?.word?.readings?.firstOrNull()
            return reading?.takeIf { it != text }
        }

    fun covers(resultId: Long, line: Int, offset: Int): Boolean =
        resultId == this.resultId && line == this.line && offset in range
}
