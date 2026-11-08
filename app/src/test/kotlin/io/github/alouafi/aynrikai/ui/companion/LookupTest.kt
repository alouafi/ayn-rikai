package io.github.alouafi.aynrikai.ui.companion

import io.github.alouafi.aynrikai.analysis.Token
import io.github.alouafi.aynrikai.dictionary.Inflection
import io.github.alouafi.aynrikai.dictionary.Word
import io.github.alouafi.aynrikai.dictionary.WordMatch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LookupTest {

    private val eat = Word(listOf("食べる"), listOf("たべる"), emptySet(), listOf("to eat"))

    private fun lookup(text: String, token: Token? = null, words: List<WordMatch> = emptyList()) =
        Lookup(resultId = 1, line = 0, range = 2..4, text = text, token = token, words = words, kanji = emptyList())

    @Test
    fun `reading comes from the model, then from an uninflected full match`() {
        assertEquals("たべた", lookup("食べた", token = Token(surface = "食べた", reading = "たべた")).reading)
        assertEquals("たべる", lookup("食べる", words = listOf(WordMatch(3, eat, emptyList()))).reading)
        assertNull(lookup("食べた", words = listOf(WordMatch(3, eat, listOf(Inflection.PAST)))).reading)
        assertNull(lookup("たべる", words = listOf(WordMatch(3, eat, emptyList()))).reading)
    }

    @Test
    fun `covers the tapped range of its own line only`() {
        val lookup = lookup("食べる")

        assertTrue(lookup.covers(resultId = 1, line = 0, offset = 3))
        assertFalse(lookup.covers(resultId = 1, line = 0, offset = 5))
        assertFalse(lookup.covers(resultId = 1, line = 1, offset = 3))
        assertFalse(lookup.covers(resultId = 2, line = 0, offset = 3))
    }
}
