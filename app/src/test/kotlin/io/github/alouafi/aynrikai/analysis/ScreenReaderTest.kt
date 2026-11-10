package io.github.alouafi.aynrikai.analysis

import io.github.alouafi.aynrikai.capture.FakeScreenImage
import io.github.alouafi.aynrikai.llm.ChatModel
import io.github.alouafi.aynrikai.llm.ChatRequest
import io.github.alouafi.aynrikai.llm.EncodedImage
import io.github.alouafi.aynrikai.ocr.TextRecognizer
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
    fun `vision reader sends the screenshot with its prompt`() = runTest {
        val image = EncodedImage(byteArrayOf(9), "image/jpeg")
        val model = RecordingModel(answer)

        val analysis = VisionScreenReader(model, AnalysisPrompt.TRANSCRIPTION).read(FakeScreenImage(image))

        val request = model.requests.single()
        assertSame(image, request.image)
        assertEquals(AnalysisPrompt.TRANSCRIPTION, request.system)
        assertEquals(AnalysisPrompt.SCREENSHOT_REQUEST, request.user)
        assertEquals("Yes", analysis.lines.single().translation)
    }

    @Test
    fun `OCR reader keeps the Japanese blocks and joins their rows`() = runTest {
        val recognizer = TextRecognizer { listOf("はい、\n わかりました。", "Start", "メニュー") }

        val analysis = OcrScreenReader(recognizer).read(FakeScreenImage())

        assertEquals(listOf("はい、わかりました。", "メニュー"), analysis.lines.map { it.japanese })
        assertTrue(analysis.lines.all { it.tokens.isEmpty() && it.translation.isEmpty() })
    }

    @Test
    fun `text analyzer sends the lines to the model as text`() = runTest {
        val model = RecordingModel(answer)

        val analysis = TextAnalyzer(model, "French").analyze(listOf("はい", "メニュー"))

        val request = model.requests.single()
        assertNull(request.image)
        assertEquals(AnalysisPrompt.forText("French"), request.system)
        assertEquals("はい\nメニュー", request.user)
        assertEquals("Yes", analysis.lines.single().translation)
    }

    @Test
    fun `text analyzer skips the model when there is no text`() = runTest {
        val model = RecordingModel(answer)

        val analysis = TextAnalyzer(model, "English").analyze(emptyList())

        assertTrue(analysis.lines.isEmpty())
        assertTrue(model.requests.isEmpty())
    }

    @Test
    fun `analyzing reader translates what the transcriber read`() = runTest {
        val model = RecordingModel(answer)
        val transcriber = ScreenReader { Analysis(listOf(Line(japanese = "はい"))) }

        val analysis = AnalyzingScreenReader(transcriber, TextAnalyzer(model, "English")).read(FakeScreenImage())

        assertEquals("はい", model.requests.single().user)
        assertEquals("Yes", analysis.lines.single().translation)
    }

    @Test
    fun `line translator asks for translations of the lines as text`() = runTest {
        val model = RecordingModel("""{"translations":["Oui","Menu"]}""")

        val translations = LineTranslator(model, "French").translate(listOf("はい", "メニュー"))

        val request = model.requests.single()
        assertNull(request.image)
        assertEquals(AnalysisPrompt.forTranslation("French"), request.system)
        assertEquals("""["はい","メニュー"]""", request.user)
        assertEquals(listOf("Oui", "Menu"), translations)
    }

    @Test
    fun `line translator skips the model when there is no text`() = runTest {
        val model = RecordingModel(answer)

        assertTrue(LineTranslator(model, "English").translate(emptyList()).isEmpty())
        assertTrue(model.requests.isEmpty())
    }
}
