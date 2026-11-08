package io.github.alouafi.aynrikai.dictionary

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Runs against the JMdict data shipped in the app. */
class WordDictionaryTest {

    private val dictionary = WordDictionary(
        openWords = { File("src/main/assets/words.tsv").inputStream() },
        openIndex = { File("src/main/assets/word_index.tsv").inputStream() },
    )

    @Test
    fun `search undoes conjugations and lists the longest match first`() = runTest {
        val matches = dictionary.search("食べさせられなかったよ")

        val longest = matches.first()
        assertEquals(10, longest.length)
        assertEquals("食べる", longest.word.headword)
        assertEquals(
            listOf(Inflection.CAUSATIVE, Inflection.POTENTIAL_OR_PASSIVE, Inflection.NEGATIVE, Inflection.PAST),
            longest.inflections,
        )
        assertTrue(longest.word.senses.first().contains("to eat"))
        assertTrue(matches.all { it.length <= longest.length })
    }

    @Test
    fun `search finds words by their kana, katakana included`() = runTest {
        val ramen = dictionary.search("ラーメン").first()
        assertEquals(4, ramen.length)
        assertEquals("拉麺", ramen.word.headword)
        assertTrue("ラーメン" in ramen.word.readings)
        assertTrue(dictionary.search("たべる").any { it.length == 3 && it.word.headword == "食べる" })
    }

    @Test
    fun `wordAt finds the group around a character`() = runTest {
        val text = "冒険者は行ってしまった。"

        assertEquals(0..2, dictionary.wordAt(text, 1))
        assertEquals(3..3, dictionary.wordAt(text, 3))
        assertEquals(4..10, dictionary.wordAt(text, 6))
        assertEquals(11..11, dictionary.wordAt(text, 11))
    }
}
