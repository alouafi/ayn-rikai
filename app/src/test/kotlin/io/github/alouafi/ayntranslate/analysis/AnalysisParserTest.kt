package io.github.alouafi.ayntranslate.analysis

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class AnalysisParserTest {

    @Test
    fun `parses an answer wrapped in a code fence and prose`() {
        val answer = """
            Here is the analysis:
            ```json
            {"lines":[{"japanese":"行こう！","translation":"Let's go!","grammar":"Volitional form.","tokens":[
              {"surface":"行こう","reading":"いこう","base":"行く","meaning":"let's go","pos":"verb"},
              {"surface":"！","reading":"","base":"","meaning":"","pos":"punctuation"}
            ]}]}
            ```
        """.trimIndent()

        val analysis = AnalysisParser.parse(answer)

        val line = analysis.lines.single()
        assertEquals("行こう！", line.japanese)
        assertEquals("Let's go!", line.translation)
        assertEquals(listOf("行こう", "！"), line.tokens.map { it.surface })
        assertEquals("行く", line.tokens[0].base)
        assertEquals(listOf(true, false), line.tokens.map { it.isWord })
    }

    @Test
    fun `fills missing and null fields with defaults`() {
        val analysis = AnalysisParser.parse("""{"lines":[{"japanese":"はい","translation":null,"extra":1}]}""")

        assertEquals(Line(japanese = "はい"), analysis.lines.single())
    }

    @Test
    fun `normalizes lines and tokens`() {
        val answer = """
            {"lines":[
              {"japanese":"","translation":"Empty line","tokens":[]},
              {"japanese":" ","translation":" Hello ","tokens":[
                {"surface":"こんにちは","reading":" こんにちは ","base":"こんにちは","meaning":"hello","pos":" Interjection "},
                {"surface":"","reading":"","base":"","meaning":"","pos":"other"}
              ]}
            ]}
        """.trimIndent()

        val line = AnalysisParser.parse(answer).lines.single()

        assertEquals("こんにちは", line.japanese)
        assertEquals("Hello", line.translation)
        val token = line.tokens.single()
        assertEquals("こんにちは", token.reading)
        assertEquals("", token.base)
        assertEquals("interjection", token.pos)
    }

    @Test
    fun `keeps spaces inside surfaces so tokens join back into the line`() {
        val answer = """{"lines":[{"japanese":"ＨＰ 回復","tokens":[{"surface":"ＨＰ "},{"surface":"回復"}]}]}"""

        val tokens = AnalysisParser.parse(answer).lines.single().tokens

        assertEquals("ＨＰ 回復", tokens.joinToString("") { it.surface })
    }

    @Test
    fun `drops lines without Japanese`() {
        val answer = """{"lines":[{"japanese":"気をつけて。"},{"japanese":"—"},{"japanese":"localhost:8765/index.html"}]}"""

        assertEquals(listOf("気をつけて。"), AnalysisParser.parse(answer).lines.map { it.japanese })
    }

    @Test
    fun `an answer without lines is empty`() {
        assertTrue(AnalysisParser.parse("""{"lines":[]}""").lines.isEmpty())
        assertTrue(AnalysisParser.parse("{}").lines.isEmpty())
    }

    @Test
    fun `rejects answers that are not an analysis`() {
        assertThrows(MalformedAnalysisException::class.java) { AnalysisParser.parse("Sorry, I cannot read this.") }
        assertThrows(MalformedAnalysisException::class.java) { AnalysisParser.parse("""{"lines":"none"}""") }
        assertThrows(MalformedAnalysisException::class.java) { AnalysisParser.parse("""{"lines":[{"japanese":""") }
    }
}
