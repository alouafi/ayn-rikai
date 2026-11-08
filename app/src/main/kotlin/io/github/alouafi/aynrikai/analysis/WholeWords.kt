package io.github.alouafi.aynrikai.analysis

import io.github.alouafi.aynrikai.dictionary.WordDictionary
import io.github.alouafi.aynrikai.dictionary.WordMatch

/**
 * Joins tokens that cut a conjugated word, like 戻っ + た, so its chip and its lookup show the whole word.
 * Whatever the prompt says, the model sometimes splits words this way when it reads text rather than a screenshot.
 */
suspend fun Analysis.withWholeWords(dictionary: WordDictionary): Analysis =
    copy(lines = lines.map { it.withWholeWords(dictionary) })

private suspend fun Line.withWholeWords(dictionary: WordDictionary): Line {
    val surface = surface
    val ranges = tokenRanges()
    val joined = mutableListOf<Token>()
    var first = 0
    while (first < tokens.size) {
        val last = if (tokens[first].isWord) lastTokenOfWord(dictionary, surface, ranges, first) else first
        joined += tokens.subList(first, last + 1).join()
        first = last + 1
    }
    return copy(tokens = joined)
}

/**
 * The last token of the longest conjugated word that starts at token [first] and ends with a token, when token
 * [first] holds the part of the word conjugation leaves alone and the next tokens only its endings. The words of
 * an expression like 噂を聞く stay apart.
 */
private suspend fun lastTokenOfWord(dictionary: WordDictionary, surface: String, ranges: List<IntRange>, first: Int): Int {
    val start = ranges[first].first
    val text = surface.substring(start)
    return dictionary.search(text)
        .filter { match -> match.inflections.isNotEmpty() && start + match.stemLength(text) <= ranges[first].last + 1 }
        .firstNotNullOfOrNull { match -> ranges.indexOfFirst { it.last == start + match.length - 1 }.takeIf { it > first } }
        ?: first
}

/** How much of the matched [text] is spelled as in the dictionary form, which conjugation leaves alone: 戻 in 戻った. */
private fun WordMatch.stemLength(text: String): Int =
    (word.spellings + word.readings).maxOf { text.take(length).commonPrefixWith(it).length }

/** The parts' readings and meanings in order, with the dictionary form and part of speech of the first part. */
private fun List<Token>.join(): Token {
    if (size == 1) return single()
    return Token(
        surface = joinToString("") { it.surface },
        reading = joinToString("") { it.reading.ifEmpty { it.surface } },
        base = first().base,
        meaning = map { it.meaning }.filter { it.isNotEmpty() }.joinToString(", "),
        pos = first().pos,
    )
}
