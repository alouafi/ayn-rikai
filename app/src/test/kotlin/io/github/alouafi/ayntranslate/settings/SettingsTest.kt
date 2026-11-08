package io.github.alouafi.ayntranslate.settings

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsTest {

    @Test
    fun `the Mistral key is needed by Mistral chat or Mistral OCR only when the pipeline uses them`() {
        assertTrue(Settings(pipeline = Pipeline.VISION, provider = Provider.MISTRAL).needsMistralKey)
        assertFalse(Settings(pipeline = Pipeline.VISION, provider = Provider.OPENAI_COMPATIBLE, ocrEngine = OcrEngine.MISTRAL).needsMistralKey)
        assertTrue(Settings(pipeline = Pipeline.OCR_ONLY, provider = Provider.MISTRAL, ocrEngine = OcrEngine.MISTRAL).needsMistralKey)
        assertFalse(Settings(pipeline = Pipeline.OCR_ONLY, provider = Provider.MISTRAL, ocrEngine = OcrEngine.ON_DEVICE).needsMistralKey)
        assertTrue(Settings(pipeline = Pipeline.OCR_THEN_LLM, provider = Provider.OPENAI_COMPATIBLE, ocrEngine = OcrEngine.MISTRAL).needsMistralKey)
    }
}
