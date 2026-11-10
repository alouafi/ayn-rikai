package io.github.alouafi.aynrikai.analysis

import io.github.alouafi.aynrikai.llm.ChatModel
import io.github.alouafi.aynrikai.llm.OpenAiCompatibleChatModel
import io.github.alouafi.aynrikai.ocr.MistralOcrRecognizer
import io.github.alouafi.aynrikai.ocr.TextRecognizer
import io.github.alouafi.aynrikai.settings.Mode
import io.github.alouafi.aynrikai.settings.Provider
import io.github.alouafi.aynrikai.settings.Settings
import io.github.alouafi.aynrikai.settings.TextSource
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient

enum class MissingSetting { MISTRAL_API_KEY, OPENAI_BASE_URL, OPENAI_MODEL }

class MissingConfigurationException(val setting: MissingSetting) : Exception("Missing setting: $setting")

/** Builds the [ScreenReader] described by the current [Settings]. User input is trimmed here and nowhere else. */
class ScreenReaderFactory(
    private val http: OkHttpClient,
    private val onDeviceRecognizer: TextRecognizer,
) {
    fun create(settings: Settings): ScreenReader = when {
        settings.mode == Mode.TRANSCRIBE -> transcriber(settings)
        // One request reads and translates the screenshot.
        settings.textSource == TextSource.VISION_MODEL ->
            VisionScreenReader(chatModel(settings), AnalysisPrompt.forScreenshot(settings.targetLanguageName()))
        else -> AnalyzingScreenReader(
            transcriber(settings),
            TextAnalyzer(chatModel(settings), settings.targetLanguageName()),
        )
    }

    /** Translates the lines of a transcription: the screenshot is not sent again. */
    fun translator(settings: Settings): LineTranslator = LineTranslator(chatModel(settings), settings.targetLanguageName())

    fun chatModel(settings: Settings): ChatModel = when (settings.provider) {
        Provider.MISTRAL -> OpenAiCompatibleChatModel(
            http,
            MISTRAL_API,
            mistralKey(settings),
            settings.mistralModel.trim().ifEmpty { Settings.DEFAULT_MISTRAL_MODEL },
            reasoningEffort = if (settings.mistralReasoning) "high" else null,
        )
        Provider.OPENAI_COMPATIBLE -> OpenAiCompatibleChatModel(
            http,
            settings.openAiBaseUrl.trim().toHttpUrlOrNull()
                ?: throw MissingConfigurationException(MissingSetting.OPENAI_BASE_URL),
            settings.openAiApiKey.trim(),
            settings.openAiModel.trim().ifEmpty { throw MissingConfigurationException(MissingSetting.OPENAI_MODEL) },
            reasoningEffort = settings.openAiReasoningEffort.apiValue,
        )
    }

    private fun transcriber(settings: Settings): ScreenReader = when (settings.textSource) {
        TextSource.VISION_MODEL -> VisionScreenReader(chatModel(settings), AnalysisPrompt.TRANSCRIPTION)
        TextSource.ON_DEVICE_OCR -> OcrScreenReader(onDeviceRecognizer)
        TextSource.MISTRAL_OCR -> OcrScreenReader(MistralOcrRecognizer(http, MISTRAL_API, mistralKey(settings)))
    }

    private fun mistralKey(settings: Settings): String =
        settings.mistralApiKey.trim().ifEmpty { throw MissingConfigurationException(MissingSetting.MISTRAL_API_KEY) }

    private companion object {
        val MISTRAL_API = "https://api.mistral.ai/v1".toHttpUrl()
    }
}
