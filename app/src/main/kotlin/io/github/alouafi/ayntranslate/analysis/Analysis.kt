package io.github.alouafi.ayntranslate.analysis

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
)

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
