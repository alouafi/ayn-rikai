package io.github.alouafi.aynrikai.dictionary

import java.lang.Character.UnicodeScript

private const val ITERATION_MARK = 0x3005 // 々 repeats the previous kanji and has no entry of its own.

fun isKanji(codePoint: Int): Boolean =
    codePoint != ITERATION_MARK && UnicodeScript.of(codePoint) == UnicodeScript.HAN

fun containsKanji(text: String): Boolean = text.codePoints().anyMatch(::isKanji)

fun isJapanese(codePoint: Int): Boolean = when (UnicodeScript.of(codePoint)) {
    UnicodeScript.HIRAGANA, UnicodeScript.KATAKANA, UnicodeScript.HAN -> true
    else -> false
}

fun containsJapanese(text: String): Boolean = text.codePoints().anyMatch(::isJapanese)

/** Katakana folded to hiragana (ラーメン → らーめん). Other characters are kept. */
fun toHiragana(text: String): String = buildString(text.length) {
    for (c in text) append(if (c in 'ァ'..'ヶ') c - KATAKANA_OFFSET else c)
}

private const val KATAKANA_OFFSET = 0x60

/** The text split into user-perceived characters, keeping surrogate pairs together. */
fun characters(text: String): List<String> = text.codePoints().toArray().map { String(Character.toChars(it)) }

/** The distinct kanji of [text], in order of appearance. */
fun kanjiIn(text: String): List<String> = characters(text).filter { isKanji(it.codePointAt(0)) }.distinct()
