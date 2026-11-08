package io.github.alouafi.ayntranslate.settings

import java.util.Locale

enum class Pipeline {
    /** One call to a vision model reads, translates and explains the screen. */
    VISION,

    /** OCR reads the text, a text model translates and explains it. */
    OCR_THEN_LLM,

    /** OCR only: Japanese text and the offline kanji dictionary, no network needed with on-device OCR. */
    OCR_ONLY,
}

enum class OcrEngine { ON_DEVICE, MISTRAL }

enum class Provider { MISTRAL, OPENAI_COMPATIBLE }

data class Settings(
    val pipeline: Pipeline = Pipeline.VISION,
    val provider: Provider = Provider.MISTRAL,
    val ocrEngine: OcrEngine = OcrEngine.ON_DEVICE,
    val mistralApiKey: String = "",
    val mistralModel: String = DEFAULT_MISTRAL_MODEL,
    val openAiBaseUrl: String = DEFAULT_OPENAI_BASE_URL,
    val openAiApiKey: String = "",
    val openAiModel: String = "",
    /** BCP 47 tag of the translation language. */
    val targetLanguage: String = defaultTargetLanguage(),
    val showFurigana: Boolean = true,
    val showRomaji: Boolean = false,
    val autoScanIntervalMs: Long = DEFAULT_AUTO_SCAN_INTERVAL_MS,
    /** Keeps the companion window from taking key focus, so the controller keeps driving the game. */
    val keepControllerOnGame: Boolean = true,
    /** Part of the game screen to read, or null for the whole screen. */
    val region: NormalizedRect? = null,
) {
    val usesOcr: Boolean get() = pipeline != Pipeline.VISION
    val usesLanguageModel: Boolean get() = pipeline != Pipeline.OCR_ONLY

    fun uses(provider: Provider): Boolean = usesLanguageModel && this.provider == provider

    fun uses(engine: OcrEngine): Boolean = usesOcr && ocrEngine == engine

    /** Mistral chat and Mistral OCR share one API key. */
    val needsMistralKey: Boolean get() = uses(Provider.MISTRAL) || uses(OcrEngine.MISTRAL)

    /** English name of the translation language, as models understand it best. */
    fun targetLanguageName(): String =
        Locale.forLanguageTag(targetLanguage).getDisplayLanguage(Locale.ENGLISH).ifEmpty { "English" }

    companion object {
        /** Small copies game text more faithfully than Medium, which tends to "fix" it. */
        const val DEFAULT_MISTRAL_MODEL = "mistral-small-latest"
        const val MEDIUM_MISTRAL_MODEL = "mistral-medium-latest"
        const val DEFAULT_OPENAI_BASE_URL = "https://api.openai.com/v1"
        const val DEFAULT_AUTO_SCAN_INTERVAL_MS = 2_000L
        val AUTO_SCAN_INTERVAL_RANGE_MS = 1_000L..10_000L

        /** Translation languages offered in the settings. */
        val LANGUAGES = listOf("en", "fr", "es", "de", "it", "pt", "nl", "pl", "ru", "uk", "zh", "ko", "ar", "tr", "id", "vi")

        private fun defaultTargetLanguage(): String =
            Locale.getDefault().language.takeIf { it in LANGUAGES } ?: "en"
    }
}
