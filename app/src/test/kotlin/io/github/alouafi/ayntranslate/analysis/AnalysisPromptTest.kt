package io.github.alouafi.ayntranslate.analysis

import org.junit.Assert.assertEquals
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
    fun `OCR prompt explains the input and keeps the answer shape`() {
        val prompt = AnalysisPrompt.forOcrText("German")

        assertTrue(prompt.contains("text that OCR read"))
        assertTrue(prompt.contains("never reword the game's text"))
        assertTrue(prompt.contains("a natural German translation"))
        assertTrue(prompt.contains("\"tokens\":[{\"surface\""))
        assertTrue(prompt.endsWith("answer {\"lines\":[]}."))
    }

    @Test
    fun `OCR request separates text boxes with a blank line`() {
        assertEquals("ひとつ\nふたつ\n\nみっつ", AnalysisPrompt.ocrRequest(listOf("ひとつ\nふたつ", "みっつ")))
    }
}
