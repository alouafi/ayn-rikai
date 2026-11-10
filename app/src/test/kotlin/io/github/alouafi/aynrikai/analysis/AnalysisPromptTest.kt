package io.github.alouafi.aynrikai.analysis

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AnalysisPromptTest {

    /** The screenshot prompt was tuned against real game screens. This fixture keeps it from drifting. */
    @Test
    fun `screenshot prompt matches the validated prompt`() {
        val expected = javaClass.getResource("/screenshot_prompt_fr.txt")!!.readText()

        assertEquals(expected, AnalysisPrompt.forScreenshot("French"))
    }

    @Test
    fun `transcription prompt asks for the Japanese text only`() {
        val prompt = AnalysisPrompt.TRANSCRIPTION

        assertTrue(prompt.contains("{\"lines\":[{\"japanese\":\"\"}]}"))
        assertTrue(prompt.contains("never correct or reword the game's text"))
        assertFalse(prompt.contains("translation"))
        assertFalse(prompt.contains("tokens"))
        // An empty-answer rule made the model skip screens full of text.
        assertFalse(prompt.contains("{\"lines\":[]}"))
    }

    @Test
    fun `text prompt explains the input and keeps the answer shape`() {
        val prompt = AnalysisPrompt.forText("German")

        assertTrue(prompt.contains("one piece of text per line"))
        assertTrue(prompt.contains("never reword the game's text"))
        assertTrue(prompt.contains("a natural German translation"))
        assertTrue(prompt.contains("\"tokens\":[{\"surface\""))
        assertTrue(prompt.endsWith("answer {\"lines\":[]}."))
    }

    @Test
    fun `text request puts one text per line`() {
        assertEquals("はい、わかりました。\nメニュー", AnalysisPrompt.textRequest(listOf("はい、わかりました。", "メニュー")))
    }

    @Test
    fun `translation prompt asks for translations only`() {
        val prompt = AnalysisPrompt.forTranslation("Spanish")

        assertTrue(prompt.contains("{\"translations\":[\"\"]}"))
        assertTrue(prompt.contains("a natural Spanish translation of each line"))
        assertTrue(prompt.contains("exactly one per line and in the same order"))
        assertFalse(prompt.contains("tokens"))
        assertFalse(prompt.contains("grammar"))
    }

    @Test
    fun `translation request is a JSON array of the lines`() {
        assertEquals(
            """["「行くぞ」と言った。","メニュー"]""",
            AnalysisPrompt.translationRequest(listOf("「行くぞ」と言った。", "メニュー")),
        )
    }
}
