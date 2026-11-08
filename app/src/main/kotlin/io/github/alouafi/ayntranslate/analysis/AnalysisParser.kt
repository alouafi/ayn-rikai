package io.github.alouafi.ayntranslate.analysis

import io.github.alouafi.ayntranslate.dictionary.containsJapanese
import kotlinx.serialization.json.Json

class MalformedAnalysisException(message: String, cause: Throwable? = null) : Exception(message, cause)

object AnalysisParser {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    /** Parses a model answer, tolerating code fences or prose around the JSON object. */
    fun parse(answer: String): Analysis {
        val start = answer.indexOf('{')
        val end = answer.lastIndexOf('}')
        if (start < 0 || end < start) throw MalformedAnalysisException("The answer contains no JSON object.")
        val analysis = try {
            json.decodeFromString<Analysis>(answer.substring(start, end + 1))
        } catch (e: IllegalArgumentException) {
            // SerializationException is an IllegalArgumentException too.
            throw MalformedAnalysisException("The answer is not a valid analysis.", e)
        }
        return analysis.normalized()
    }
}

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
