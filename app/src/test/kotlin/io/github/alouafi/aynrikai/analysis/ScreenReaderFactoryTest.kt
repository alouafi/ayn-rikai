package io.github.alouafi.aynrikai.analysis

import io.github.alouafi.aynrikai.ocr.TextRecognizer
import io.github.alouafi.aynrikai.settings.Mode
import io.github.alouafi.aynrikai.settings.Provider
import io.github.alouafi.aynrikai.settings.Settings
import io.github.alouafi.aynrikai.settings.TextSource
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class ScreenReaderFactoryTest {

    private val factory = ScreenReaderFactory(OkHttpClient(), TextRecognizer { emptyList() })

    private fun missing(settings: Settings): MissingSetting =
        assertThrows(MissingConfigurationException::class.java) { factory.create(settings) }.setting

    @Test
    fun `Mistral needs an API key`() {
        assertEquals(MissingSetting.MISTRAL_API_KEY, missing(Settings(mistralApiKey = "  ")))
        assertEquals(
            MissingSetting.MISTRAL_API_KEY,
            missing(Settings(mode = Mode.TRANSCRIBE, textSource = TextSource.MISTRAL_OCR)),
        )
    }

    @Test
    fun `OpenAI-compatible servers need a valid URL and a model, but no key`() {
        val openAi = Settings(provider = Provider.OPENAI_COMPATIBLE, openAiModel = "qwen2.5vl")

        assertEquals(MissingSetting.OPENAI_BASE_URL, missing(openAi.copy(openAiBaseUrl = "localhost:11434")))
        assertEquals(MissingSetting.OPENAI_MODEL, missing(openAi.copy(openAiModel = " ")))
        assertTrue(factory.create(openAi.copy(openAiBaseUrl = "http://192.168.1.20:11434/v1")) is VisionScreenReader)
    }

    @Test
    fun `translate mode reads and translates in one request only with the vision model`() {
        val settings = Settings(mode = Mode.TRANSLATE, mistralApiKey = "key")

        assertTrue(factory.create(settings) is VisionScreenReader)
        assertTrue(factory.create(settings.copy(textSource = TextSource.ON_DEVICE_OCR)) is AnalyzingScreenReader)
        assertTrue(factory.create(settings.copy(textSource = TextSource.MISTRAL_OCR)) is AnalyzingScreenReader)
    }

    @Test
    fun `transcribe mode only reads the text`() {
        val settings = Settings(mode = Mode.TRANSCRIBE, mistralApiKey = "key")

        assertTrue(factory.create(settings) is VisionScreenReader)
        assertTrue(factory.create(settings.copy(textSource = TextSource.MISTRAL_OCR)) is OcrScreenReader)
    }

    @Test
    fun `on-device transcription needs no configuration`() {
        val reader = factory.create(Settings(mode = Mode.TRANSCRIBE, textSource = TextSource.ON_DEVICE_OCR))

        assertTrue(reader is OcrScreenReader)
    }
}
