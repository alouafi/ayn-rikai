package io.github.alouafi.ayntranslate.analysis

import io.github.alouafi.ayntranslate.dictionary.containsJapanese
import io.github.alouafi.ayntranslate.llm.ChatModel
import io.github.alouafi.ayntranslate.llm.ChatRequest
import io.github.alouafi.ayntranslate.ocr.TextRecognizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Turns a game screen into lines, translations and words. */
fun interface ScreenReader {
    suspend fun read(screen: ScreenImage): Analysis
}

/** Sends the screenshot to a vision model, which reads and explains the text in one call. */
class VisionScreenReader(private val model: ChatModel, private val language: String) : ScreenReader {
    override suspend fun read(screen: ScreenImage): Analysis {
        val image = withContext(Dispatchers.Default) { screen.jpeg() }
        val answer = model.complete(
            ChatRequest(
                system = AnalysisPrompt.forScreenshot(language),
                user = AnalysisPrompt.SCREENSHOT_REQUEST,
                image = image,
            ),
        )
        return AnalysisParser.parse(answer)
    }
}

/**
 * Reads the text with OCR, then asks a text model to translate and split it.
 * Without a model, it returns the Japanese text only, so the kanji dictionary still works offline.
 */
class OcrScreenReader(
    private val recognizer: TextRecognizer,
    private val model: ChatModel?,
    private val language: String,
) : ScreenReader {
    override suspend fun read(screen: ScreenImage): Analysis {
        val blocks = recognizer.recognize(screen).filter(::containsJapanese)
        if (blocks.isEmpty()) return Analysis()
        if (model == null) return Analysis(blocks.map { Line(japanese = joinRows(it)) })

        val answer = model.complete(
            ChatRequest(system = AnalysisPrompt.forOcrText(language), user = AnalysisPrompt.ocrRequest(blocks)),
        )
        return AnalysisParser.parse(answer)
    }

    /** Japanese wraps without spaces, so the rows of one text box join back without separators. */
    private fun joinRows(block: String): String = block.lines().joinToString("") { it.trim() }
}
