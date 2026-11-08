package io.github.alouafi.ayntranslate.dictionary

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class KanjiDictionaryTest {

    private val tsv = listOf(
        "# KANJIDIC2 test data",
        "語\t14\t2\t301\t149\tゴ\tかた.る|かた.らう\tword|speech|language\tlangage|mot|raconter",
        "鬱\t29\t\t\t192\tウツ\tふさ.ぐ|しげ.る\tgloom|depression\t",
        "broken\trow",
    ).joinToString("\n")

    @Test
    fun `parses rows with empty fields and skips comments and broken rows`() {
        val entries = parseKanjiTsv(tsv.lineSequence())

        assertEquals(setOf("語", "鬱"), entries.keys)
        val word = entries.getValue("語")
        assertEquals(14, word.strokes)
        assertEquals(2, word.grade)
        assertEquals(301, word.frequency)
        assertEquals(listOf("ゴ"), word.onReadings)
        assertEquals(listOf("かた（る）", "かた（らう）"), word.kunReadings)
        val gloom = entries.getValue("鬱")
        assertNull(gloom.grade)
        assertNull(gloom.frequency)
        assertTrue(gloom.meaningsFr.isEmpty())
    }

    @Test
    fun `kun readings show okurigana in brackets and keep prefix marks`() {
        val one = parseKanjiTsv(sequenceOf("一\t1\t1\t2\t1\tイチ\tひと-|ひと.つ|いち\tone\tun")).getValue("一")

        assertEquals(listOf("ひと-", "ひと（つ）", "いち"), one.kunReadings)
    }

    @Test
    fun `meanings follow the language and fall back to English`() {
        val entries = parseKanjiTsv(tsv.lineSequence())

        assertEquals(listOf("langage", "mot", "raconter"), entries.getValue("語").meanings("fr-CA"))
        assertEquals(listOf("word", "speech", "language"), entries.getValue("語").meanings("de"))
        assertEquals(listOf("gloom", "depression"), entries.getValue("鬱").meanings("fr"))
    }

    @Test
    fun `radical symbols come from the Kangxi radicals block`() {
        val kanji = parseKanjiTsv(tsv.lineSequence()).getValue("語")

        assertEquals("⾔", kanji.radicalSymbol)
        assertEquals("⼀", kanji.copy(radical = 1).radicalSymbol)
        assertNull(kanji.copy(radical = 215).radicalSymbol)
        assertNull(kanji.copy(radical = null).radicalSymbol)
    }

    @Test
    fun `dictionary loads its data once`() = runTest {
        var opens = 0
        val dictionary = KanjiDictionary {
            opens++
            tsv.byteInputStream()
        }

        assertEquals(14, dictionary.lookup("語")?.strokes)
        assertNull(dictionary.lookup("x"))
        assertEquals(1, opens)
    }

    @Test
    fun `bundled asset covers common kanji`() {
        val entries = File("src/main/assets/kanji.tsv").useLines(block = ::parseKanjiTsv)

        assertTrue(entries.size > 13_000)
        val word = entries.getValue("語")
        assertEquals(listOf("ゴ"), word.onReadings)
        assertTrue("word" in word.meaningsEn)
        assertTrue("mot" in word.meaningsFr)
    }
}
