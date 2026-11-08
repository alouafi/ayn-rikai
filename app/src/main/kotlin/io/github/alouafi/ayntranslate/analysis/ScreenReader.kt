package io.github.alouafi.ayntranslate.analysis

import io.github.alouafi.ayntranslate.capture.ScreenImage
import io.github.alouafi.ayntranslate.dictionary.containsJapanese
import io.github.alouafi.ayntranslate.llm.ChatModel
import io.github.alouafi.ayntranslate.llm.ChatRequest
import io.github.alouafi.ayntranslate.ocr.TextRecognizer

/** Turns a game screen into lines, translations and words. */
fun interface ScreenReader {
    suspend fun read(screen: ScreenImage): Analysis
}

/** Sends the screenshot to a vision model, which reads and explains the text in one call. */
class VisionScreenReader(private val model: ChatModel, private val language: String) : ScreenReader {
    override suspend fun read(screen: ScreenImage): Analysis {
        val answer = model.complete(
            ChatRequest(
                system = AnalysisPrompt.forScreenshot(language),
                user = AnalysisPrompt.SCREENSHOT_REQUEST,
                image = screen.jpeg(),
            ),
        )
        return AnalysisParser.parse(answer)
    }
}

/** Reads the text with OCR, then asks a text model to translate and split it. */
class OcrScreenReader(
    private val recognizer: TextRecognizer,
    private val model: ChatModel,
    private val language: String,
) : ScreenReader {
    override suspend fun read(screen: ScreenImage): Analysis {
        val blocks = recognizer.japaneseBlocks(screen)
        if (blocks.isEmpty()) return Analysis()
        val answer = model.complete(
            ChatRequest(system = AnalysisPrompt.forOcrText(language), user = AnalysisPrompt.ocrRequest(blocks)),
        )
        return AnalysisParser.parse(answer)
    }
}

/** Returns the Japanese text only, without any model, so the kanji dictionary still works offline. */
class OcrOnlyScreenReader(private val recognizer: TextRecognizer) : ScreenReader {
    override suspend fun read(screen: ScreenImage): Analysis =
        Analysis(recognizer.japaneseBlocks(screen).map { Line(japanese = joinRows(it)) })

    /** Japanese wraps without spaces, so the rows of one text box join back without separators. */
    private fun joinRows(block: String): String = block.lines().joinToString("") { it.trim() }
}

private suspend fun TextRecognizer.japaneseBlocks(screen: ScreenImage): List<String> =
    recognize(screen).filter(::containsJapanese)
