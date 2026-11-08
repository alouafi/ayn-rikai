package io.github.alouafi.aynrikai.analysis

import io.github.alouafi.aynrikai.capture.ScreenImage
import io.github.alouafi.aynrikai.dictionary.containsJapanese
import io.github.alouafi.aynrikai.llm.ChatModel
import io.github.alouafi.aynrikai.llm.ChatRequest
import io.github.alouafi.aynrikai.ocr.TextRecognizer

/** Turns a game screen into lines of Japanese text, translated and split into words or not. */
fun interface ScreenReader {
    suspend fun read(screen: ScreenImage): Analysis
}

/** Sends the screenshot to a vision model with [prompt], which asks for a transcription or a full analysis. */
class VisionScreenReader(private val model: ChatModel, private val prompt: String) : ScreenReader {
    override suspend fun read(screen: ScreenImage): Analysis {
        val answer = model.complete(
            ChatRequest(system = prompt, user = AnalysisPrompt.SCREENSHOT_REQUEST, image = screen.jpeg()),
        )
        return AnalysisParser.parse(answer)
    }
}

/** Reads the Japanese text with OCR, one line per text box. */
class OcrScreenReader(private val recognizer: TextRecognizer) : ScreenReader {
    override suspend fun read(screen: ScreenImage): Analysis =
        Analysis(recognizer.recognize(screen).filter(::containsJapanese).map { Line(japanese = joinRows(it)) })

    /** Japanese wraps without spaces, so the rows of one text box join back without separators. */
    private fun joinRows(block: String): String = block.lines().joinToString("") { it.trim() }
}

/** Translates and explains Japanese text that was already read. */
class TextAnalyzer(private val model: ChatModel, private val language: String) {
    suspend fun analyze(texts: List<String>): Analysis {
        if (texts.isEmpty()) return Analysis()
        val answer = model.complete(
            ChatRequest(system = AnalysisPrompt.forText(language), user = AnalysisPrompt.textRequest(texts)),
        )
        return AnalysisParser.parse(answer)
    }
}

/** Reads the text with [transcriber], then translates it with [analyzer]. */
class AnalyzingScreenReader(private val transcriber: ScreenReader, private val analyzer: TextAnalyzer) : ScreenReader {
    override suspend fun read(screen: ScreenImage): Analysis =
        analyzer.analyze(transcriber.read(screen).lines.map { it.japanese })
}
