package io.github.alouafi.aynrikai.dictionary

import io.github.alouafi.aynrikai.dictionary.Inflection.CAUSATIVE
import io.github.alouafi.aynrikai.dictionary.Inflection.COMPLETION
import io.github.alouafi.aynrikai.dictionary.Inflection.NEGATIVE
import io.github.alouafi.aynrikai.dictionary.Inflection.PAST
import io.github.alouafi.aynrikai.dictionary.Inflection.POLITE
import io.github.alouafi.aynrikai.dictionary.Inflection.POTENTIAL_OR_PASSIVE
import io.github.alouafi.aynrikai.dictionary.Inflection.PROGRESSIVE
import io.github.alouafi.aynrikai.dictionary.Inflection.TE
import io.github.alouafi.aynrikai.dictionary.Inflection.WANT
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DeinflectorTest {

    /** The inflections that lead from a word of class [form] written [base] to [text], or null when none do. */
    private fun inflections(text: String, base: String, form: Form): List<Inflection>? =
        Deinflector.deinflect(text).firstOrNull { it.text == base && it.forms != null && it.accepts(setOf(form)) }?.inflections

    @Test
    fun `the text as written is the first candidate and fits any word`() {
        val first = Deinflector.deinflect("食べる").first()

        assertEquals(Deinflection("食べる", null, emptyList()), first)
        assertTrue(first.accepts(emptySet()))
    }

    @Test
    fun `undoes chains of endings, outermost first`() {
        assertEquals(listOf(NEGATIVE, PAST), inflections("食べなかった", "食べる", Form.ICHIDAN))
        assertEquals(
            listOf(CAUSATIVE, POTENTIAL_OR_PASSIVE, NEGATIVE, PAST),
            inflections("食べさせられなかった", "食べる", Form.ICHIDAN),
        )
        assertEquals(listOf(WANT, NEGATIVE), inflections("飲みたくない", "飲む", Form.GODAN))
    }

    @Test
    fun `te forms of godan verbs, including the irregular 行く`() {
        assertEquals(listOf(TE), inflections("書いて", "書く", Form.GODAN))
        assertEquals(listOf(TE), inflections("行って", "行く", Form.IKU))
        assertEquals(listOf(PROGRESSIVE), inflections("書いている", "書く", Form.GODAN))
        assertEquals(listOf(COMPLETION, PAST), inflections("行っちゃった", "行く", Form.IKU))
    }

    @Test
    fun `suru nouns and kuru`() {
        assertEquals(listOf(PAST), inflections("勉強した", "勉強", Form.SURU_NOUN))
        assertEquals(listOf(POLITE), inflections("きます", "くる", Form.KURU))
        assertEquals(listOf(POLITE), inflections("きます", "きる", Form.ICHIDAN))
    }

    @Test
    fun `stems are never words on their own`() {
        assertFalse(Deinflector.deinflect("食べます").any { it.text == "食べ" && it.accepts(setOf(Form.ICHIDAN)) })
    }
}
