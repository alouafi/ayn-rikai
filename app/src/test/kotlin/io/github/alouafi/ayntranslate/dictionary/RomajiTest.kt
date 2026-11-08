package io.github.alouafi.ayntranslate.dictionary

import org.junit.Assert.assertEquals
import org.junit.Test

class RomajiTest {

    private fun assertRomaji(expected: String, kana: String) = assertEquals(kana, expected, Romaji.fromKana(kana))

    @Test
    fun `basic syllables and digraphs`() {
        assertRomaji("arigatou", "ありがとう")
        assertRomaji("kyou", "きょう")
        assertRomaji("shashin", "しゃしん")
        assertRomaji("jinja", "じんじゃ")
        assertRomaji("tsuzuku", "つづく")
    }

    @Test
    fun `small tsu doubles the next consonant`() {
        assertRomaji("gakkou", "がっこう")
        assertRomaji("chotto", "ちょっと")
        assertRomaji("matcha", "まっちゃ")
        assertRomaji("zasshi", "ざっし")
    }

    @Test
    fun `syllabic n takes an apostrophe before a vowel or y`() {
        assertRomaji("hon'ya", "ほんや")
        assertRomaji("kan'i", "かんい")
        assertRomaji("shinbun", "しんぶん")
        assertRomaji("onna", "おんな")
    }

    @Test
    fun `katakana and long vowel marks`() {
        assertRomaji("raamen", "ラーメン")
        assertRomaji("konpyuutaa", "コンピューター")
        assertRomaji("fairu", "ファイル")
        assertRomaji("paatii", "パーティー")
        assertRomaji("chekku", "チェック")
    }

    @Test
    fun `characters that are not kana pass through`() {
        assertRomaji("東京he!", "東京へ!")
    }
}
