package io.github.alouafi.ayntranslate.analysis

import io.github.alouafi.ayntranslate.capture.FakeScreenImage
import io.github.alouafi.ayntranslate.llm.ChatModel
import io.github.alouafi.ayntranslate.llm.ChatRequest
import io.github.alouafi.ayntranslate.llm.EncodedImage
import io.github.alouafi.ayntranslate.ocr.TextRecognizer
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class ScreenReaderTest {

    private val answer = """{"lines":[{"japanese":"はい","translation":"Yes","tokens":[{"surface":"はい","pos":"interjection"}]}]}"""

    private class RecordingModel(private val answer: String) : ChatModel {
        val requests = mutableListOf<ChatRequest>()

        override suspend fun complete(request: ChatRequest): String {
            requests += request
            return answer
        }
    }

    @Test
    fun `vision reader sends the screenshot with the screenshot prompt`() = runTest {
        val image = EncodedImage(byteArrayOf(9), "image/jpeg")
        val model = RecordingModel(answer)

        val analysis = VisionScreenReader(model, "English").read(FakeScreenImage(image))

        val request = model.requests.single()
        assertSame(image, request.image)
        assertEquals(AnalysisPrompt.forScreenshot("English"), request.system)
        assertEquals(AnalysisPrompt.SCREENSHOT_REQUEST, request.user)
        assertEquals("Yes", analysis.lines.single().translation)
    }

    @Test
    fun `OCR reader sends the Japanese blocks to the model as text`() = runTest {
        val recognizer = TextRecognizer { listOf("HP 120/120", "はい、\nわかりました。", "LV 5") }
        val model = RecordingModel(answer)

        val analysis = OcrScreenReader(recognizer, model, "French").read(FakeScreenImage())

        val request = model.requests.single()
        assertNull(request.image)
        assertEquals(AnalysisPrompt.forOcrText("French"), request.system)
        assertEquals("はい、\nわかりました。", request.user)
        assertEquals("はい", analysis.lines.single().japanese)
    }

    @Test
    fun `OCR-only reader returns the Japanese text only`() = runTest {
        val recognizer = TextRecognizer { listOf("はい、\n わかりました。", "Start", "メニュー") }

        val analysis = OcrOnlyScreenReader(recognizer).read(FakeScreenImage())

        assertEquals(listOf("はい、わかりました。", "メニュー"), analysis.lines.map { it.japanese })
        assertTrue(analysis.lines.all { it.tokens.isEmpty() && it.translation.isEmpty() })
    }

    @Test
    fun `OCR reader skips the model when there is no Japanese text`() = runTest {
        val model = RecordingModel(answer)

        val analysis = OcrScreenReader({ listOf("PAUSE") }, model, "English").read(FakeScreenImage())

        assertTrue(analysis.lines.isEmpty())
        assertTrue(model.requests.isEmpty())
    }
}
