package io.github.alouafi.aynrikai.analysis

import kotlinx.serialization.Serializable

/** What the reader found on one game screen. */
@Serializable
data class Analysis(val lines: List<Line> = emptyList())

@Serializable
data class Line(
    val japanese: String = "",
    val translation: String = "",
    val grammar: String = "",
    val tokens: List<Token> = emptyList(),
) {
    /** The Japanese text as the line card shows it, which tap offsets point into: the tokens when the model split it. */
    val surface: String
        get() = if (tokens.isEmpty()) japanese else tokens.joinToString("") { it.surface }

    /** Where each token is in [surface]. */
    fun tokenRanges(): List<IntRange> =
        tokens.runningFold(0) { start, token -> start + token.surface.length }.zipWithNext { start, end -> start until end }
}

@Serializable
data class Token(
    val surface: String = "",
    /** Hiragana reading, katakana for katakana words, empty for punctuation. */
    val reading: String = "",
    /** Dictionary form when the surface is conjugated, otherwise empty. */
    val base: String = "",
    val meaning: String = "",
    val pos: String = "",
) {
    val isWord: Boolean
        get() = pos != "punctuation" && surface.any { it.isLetterOrDigit() }
}
