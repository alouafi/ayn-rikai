package io.github.alouafi.aynrikai.analysis

import io.github.alouafi.aynrikai.dictionary.containsJapanese
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class MalformedAnalysisException(message: String, cause: Throwable? = null) : Exception(message, cause)

object AnalysisParser {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    /** Parses a model answer, tolerating code fences or prose around the JSON object. */
    fun parse(answer: String): Analysis = decode<Analysis>(answer).normalized()

    /** Parses the answer to a translation request for [count] lines: one translation per line, in order. */
    fun parseTranslations(answer: String, count: Int): List<String> {
        val translations = decode<Translations>(answer).translations.map { it.trim() }
        if (translations.size != count) {
            throw MalformedAnalysisException("Expected $count translations, got ${translations.size}.")
        }
        return translations
    }

    private inline fun <reified T> decode(answer: String): T {
        val start = answer.indexOf('{')
        val end = answer.lastIndexOf('}')
        if (start < 0 || end < start) throw MalformedAnalysisException("The answer contains no JSON object.")
        return try {
            json.decodeFromString<T>(answer.substring(start, end + 1))
        } catch (e: IllegalArgumentException) {
            // SerializationException is an IllegalArgumentException too.
            throw MalformedAnalysisException("The answer does not have the requested shape.", e)
        }
    }
}

@Serializable
private class Translations(val translations: List<String> = emptyList())

/** Drops lines without Japanese, such as a URL or a dash that the model picked up despite the prompt. */
internal fun Analysis.normalized(): Analysis =
    Analysis(lines.map { it.normalized() }.filter { containsJapanese(it.japanese) })

private fun Line.normalized(): Line {
    val tokens = tokens.map { it.normalized() }.filter { it.surface.isNotEmpty() }
    return Line(
        japanese = japanese.trim().ifEmpty { tokens.joinToString("") { it.surface }.trim() },
        translation = translation.trim(),
        grammar = grammar.trim(),
        tokens = tokens,
    )
}

private fun Token.normalized(): Token {
    val base = base.trim()
    return Token(
        surface = surface,
        reading = reading.trim(),
        base = if (base == surface) "" else base,
        meaning = meaning.trim(),
        pos = pos.trim().lowercase(),
    )
}
