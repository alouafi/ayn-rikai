package io.github.alouafi.aynrikai.analysis

import io.github.alouafi.aynrikai.dictionary.WordDictionary
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

/** Runs against the JMdict data shipped in the app. */
class WholeWordsTest {

    private val dictionary = WordDictionary(
        openWords = { File("src/main/assets/words.tsv").inputStream() },
        openIndex = { File("src/main/assets/word_index.tsv").inputStream() },
    )

    private fun line(vararg tokens: Token) = Line(japanese = tokens.joinToString("") { it.surface }, tokens = tokens.toList())

    @Test
    fun `token lines tap into the joined surfaces`() {
        val line = Line(japanese = "猫が いる", tokens = listOf(Token(surface = "猫"), Token(surface = "が"), Token(surface = "いる")))

        assertEquals("猫がいる", line.surface)
        assertEquals(listOf(0..0, 1..1, 2..3), line.tokenRanges())
        assertEquals("猫が いる", Line(japanese = "猫が いる").surface)
    }

    @Test
    fun `joins the tokens of a conjugated word the model split`() = runTest {
        val analysis = Analysis(
            listOf(
                line(
                    Token("勇者", "ゆうしゃ", "", "héros", "noun"),
                    Token("よ", "よ", "", "", "particle"),
                    Token("、", pos = "punctuation"),
                    Token("戻っ", "もどっ", "戻る", "revenir", "verb"),
                    Token("た", "た", "", "passé", "auxiliary"),
                    Token("か", "か", "", "question", "particle"),
                ),
                line(
                    Token("魔物", "まもの", "", "monstre", "noun"),
                    Token("が", "が", "", "", "particle"),
                    Token("住み着い", "すみつい", "住み着く", "s'installer", "verb"),
                    Token("て", "", "", "", "particle"),
                    Token("い", "い", "いる", "", "auxiliary"),
                    Token("た", "た", "", "", "auxiliary"),
                ),
                line(
                    Token("噂", "うわさ", "", "rumeur", "noun"),
                    Token("を", "を", "", "", "particle"),
                    Token("聞い", "きい", "聞く", "entendre", "verb"),
                    Token("た", "た", "", "", "auxiliary"),
                    Token("食べ", "たべ", "食べる", "manger", "verb"),
                    Token("た", "た", "", "", "auxiliary"),
                ),
            ),
        )

        val lines = analysis.withWholeWords(dictionary).lines

        assertEquals(listOf("勇者", "よ", "、", "戻った", "か"), lines[0].tokens.map { it.surface })
        assertEquals(Token("戻った", "もどった", "戻る", "revenir, passé", "verb"), lines[0].tokens[3])
        assertEquals(listOf("魔物", "が", "住み着いていた"), lines[1].tokens.map { it.surface })
        assertEquals("すみついていた", lines[1].tokens[2].reading)
        // The words of the expression 噂を聞く stay apart, while an ichidan stem joins its ending.
        assertEquals(listOf("噂", "を", "聞いた", "食べた"), lines[2].tokens.map { it.surface })
        assertEquals(analysis.lines.map { it.surface }, lines.map { it.surface })
    }

    @Test
    fun `keeps whole words and lines without tokens as they are`() = runTest {
        val analysis = Analysis(
            listOf(
                line(
                    Token("冒険者", "ぼうけんしゃ", "", "aventurier", "noun"),
                    Token("は", "は", "", "", "particle"),
                    Token("行ってしまった", "いってしまった", "行く", "est parti", "verb"),
                    Token("。", pos = "punctuation"),
                ),
                Line(japanese = "戻っ た"),
            ),
        )

        assertEquals(analysis, analysis.withWholeWords(dictionary))
    }
}
