package io.github.alouafi.aynrikai.settings

import java.util.Locale

enum class Mode {
    /** Scans give the Japanese text only. Each result has a button to translate it. */
    TRANSCRIBE,

    /** Scans give the text with its translation, grammar notes and words. */
    TRANSLATE,
}

enum class TextSource {
    /** The language model reads the screenshot. In translate mode the same request also translates it. */
    VISION_MODEL,

    /** ML Kit reads the text on the device: free, offline and instant. */
    ON_DEVICE_OCR,

    /** Mistral OCR reads the text: better on small and pixel fonts. */
    MISTRAL_OCR,
}

enum class Provider { MISTRAL, OPENAI_COMPATIBLE }

/** The `reasoning_effort` sent to OpenAI-compatible servers. [DEFAULT] sends none and keeps the model's own. */
enum class ReasoningEffort(val apiValue: String?) {
    DEFAULT(null),
    NONE("none"),
    LOW("low"),
    MEDIUM("medium"),
    HIGH("high"),
}

data class Settings(
    val mode: Mode = Mode.TRANSLATE,
    val textSource: TextSource = TextSource.VISION_MODEL,
    val provider: Provider = Provider.MISTRAL,
    val mistralApiKey: String = "",
    val mistralModel: String = DEFAULT_MISTRAL_MODEL,
    /** Mistral models reason at one level, `high`, or not at all, which is their default. */
    val mistralReasoning: Boolean = false,
    val openAiBaseUrl: String = DEFAULT_OPENAI_BASE_URL,
    val openAiApiKey: String = "",
    val openAiModel: String = "",
    val openAiReasoningEffort: ReasoningEffort = ReasoningEffort.DEFAULT,
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
    /** Mistral chat and Mistral OCR share one API key. */
    val needsMistralKey: Boolean get() = provider == Provider.MISTRAL || textSource == TextSource.MISTRAL_OCR

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
