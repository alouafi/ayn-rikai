package io.github.alouafi.ayntranslate.dictionary

import java.lang.Character.UnicodeScript

private const val ITERATION_MARK = 0x3005 // 々 repeats the previous kanji and has no entry of its own.

fun isKanji(codePoint: Int): Boolean =
    codePoint != ITERATION_MARK && UnicodeScript.of(codePoint) == UnicodeScript.HAN

fun containsKanji(text: String): Boolean = text.codePoints().anyMatch(::isKanji)

fun containsJapanese(text: String): Boolean = text.codePoints().anyMatch {
    when (UnicodeScript.of(it)) {
        UnicodeScript.HIRAGANA, UnicodeScript.KATAKANA, UnicodeScript.HAN -> true
        else -> false
    }
}

/** The text split into user-perceived characters, keeping surrogate pairs together. */
fun characters(text: String): List<String> = text.codePoints().toArray().map { String(Character.toChars(it)) }

/** The distinct kanji of [text], in order of appearance. */
fun kanjiIn(text: String): List<String> = characters(text).filter { isKanji(it.codePointAt(0)) }.distinct()
