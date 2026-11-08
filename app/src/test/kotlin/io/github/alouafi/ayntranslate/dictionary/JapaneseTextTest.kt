package io.github.alouafi.ayntranslate.dictionary

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class JapaneseTextTest {

    @Test
    fun `kanji detection excludes kana and the iteration mark`() {
        assertTrue(isKanji('漢'.code))
        assertFalse(isKanji('あ'.code))
        assertFalse(isKanji('ア'.code))
        assertFalse(isKanji('々'.code))
        assertTrue(containsKanji("ひと月"))
        assertFalse(containsKanji("ひらがな"))
    }

    @Test
    fun `Japanese detection covers kana and kanji only`() {
        assertTrue(containsJapanese("HP回復"))
        assertTrue(containsJapanese("カタカナ"))
        assertFalse(containsJapanese("LV 99 / Ｇ"))
    }

    @Test
    fun `characters keep surrogate pairs together`() {
        assertEquals(listOf("𠮟", "る"), characters("𠮟る"))
    }

    @Test
    fun `kanjiIn lists distinct kanji in order`() {
        assertEquals(listOf("日", "本", "語"), kanjiIn("日本の日々、日本語"))
    }
}
