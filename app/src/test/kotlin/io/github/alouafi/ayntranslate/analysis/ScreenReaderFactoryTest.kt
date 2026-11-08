package io.github.alouafi.ayntranslate.analysis

import io.github.alouafi.ayntranslate.ocr.TextRecognizer
import io.github.alouafi.ayntranslate.settings.OcrEngine
import io.github.alouafi.ayntranslate.settings.Pipeline
import io.github.alouafi.ayntranslate.settings.Provider
import io.github.alouafi.ayntranslate.settings.Settings
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
            missing(Settings(pipeline = Pipeline.OCR_ONLY, ocrEngine = OcrEngine.MISTRAL)),
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
    fun `on-device OCR alone needs no configuration`() {
        val reader = factory.create(Settings(pipeline = Pipeline.OCR_ONLY, ocrEngine = OcrEngine.ON_DEVICE))

        assertTrue(reader is OcrOnlyScreenReader)
    }
}
