package io.github.alouafi.ayntranslate.analysis

import io.github.alouafi.ayntranslate.llm.ChatModel
import io.github.alouafi.ayntranslate.llm.OpenAiCompatibleChatModel
import io.github.alouafi.ayntranslate.ocr.MistralOcrRecognizer
import io.github.alouafi.ayntranslate.ocr.TextRecognizer
import io.github.alouafi.ayntranslate.settings.OcrEngine
import io.github.alouafi.ayntranslate.settings.Pipeline
import io.github.alouafi.ayntranslate.settings.Provider
import io.github.alouafi.ayntranslate.settings.Settings
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient

enum class MissingSetting { MISTRAL_API_KEY, OPENAI_BASE_URL, OPENAI_MODEL }

class MissingConfigurationException(val setting: MissingSetting) : Exception("Missing setting: $setting")

/** Builds the [ScreenReader] described by the current [Settings]. */
class ScreenReaderFactory(
    private val http: OkHttpClient,
    private val onDeviceRecognizer: () -> TextRecognizer,
) {
    fun create(settings: Settings): ScreenReader {
        val language = settings.targetLanguageName()
        return when (settings.pipeline) {
            Pipeline.VISION -> VisionScreenReader(chatModel(settings), language)
            Pipeline.OCR_THEN_LLM -> OcrScreenReader(recognizer(settings), chatModel(settings), language)
            Pipeline.OCR_ONLY -> OcrScreenReader(recognizer(settings), model = null, language)
        }
    }

    fun chatModel(settings: Settings): ChatModel = when (settings.provider) {
        Provider.MISTRAL -> OpenAiCompatibleChatModel(
            http,
            MISTRAL_BASE_URL.toHttpUrl(),
            mistralKey(settings),
            settings.mistralModel.trim().ifEmpty { Settings.DEFAULT_MISTRAL_MODEL },
        )
        Provider.OPENAI_COMPATIBLE -> OpenAiCompatibleChatModel(
            http,
            settings.openAiBaseUrl.trim().toHttpUrlOrNull()
                ?: throw MissingConfigurationException(MissingSetting.OPENAI_BASE_URL),
            settings.openAiApiKey,
            settings.openAiModel.trim().ifEmpty { throw MissingConfigurationException(MissingSetting.OPENAI_MODEL) },
        )
    }

    private fun recognizer(settings: Settings): TextRecognizer = when (settings.ocrEngine) {
        OcrEngine.ON_DEVICE -> onDeviceRecognizer()
        OcrEngine.MISTRAL -> MistralOcrRecognizer(
            http,
            MISTRAL_BASE_URL.toHttpUrl(),
            mistralKey(settings),
            settings.mistralOcrModel.trim().ifEmpty { Settings.DEFAULT_MISTRAL_OCR_MODEL },
        )
    }

    private fun mistralKey(settings: Settings): String =
        settings.mistralApiKey.trim().ifEmpty { throw MissingConfigurationException(MissingSetting.MISTRAL_API_KEY) }

    companion object {
        const val MISTRAL_BASE_URL = "https://api.mistral.ai/v1"
    }
}
