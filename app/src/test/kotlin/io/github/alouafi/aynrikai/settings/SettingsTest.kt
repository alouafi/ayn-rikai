package io.github.alouafi.aynrikai.settings

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsTest {

    @Test
    fun `the Mistral key is needed by Mistral chat or Mistral OCR`() {
        assertTrue(Settings(provider = Provider.MISTRAL, textSource = TextSource.ON_DEVICE_OCR).needsMistralKey)
        assertTrue(Settings(provider = Provider.OPENAI_COMPATIBLE, textSource = TextSource.MISTRAL_OCR).needsMistralKey)
        assertFalse(Settings(provider = Provider.OPENAI_COMPATIBLE, textSource = TextSource.VISION_MODEL).needsMistralKey)
        assertFalse(Settings(provider = Provider.OPENAI_COMPATIBLE, textSource = TextSource.ON_DEVICE_OCR).needsMistralKey)
    }
}
