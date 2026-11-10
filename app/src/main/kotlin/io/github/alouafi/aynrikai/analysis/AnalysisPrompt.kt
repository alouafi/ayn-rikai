package io.github.alouafi.aynrikai.analysis

import kotlinx.serialization.json.Json

/** Prompts that ask a model for an [Analysis], or for translations, as JSON. `language` is an English language name. */
object AnalysisPrompt {

    const val SCREENSHOT_REQUEST = "Read the Japanese text on this game screen."

    /** Reads, translates and explains a screenshot in one request. */
    fun forScreenshot(language: String): String =
        prompt(SCREENSHOT, ANALYSIS_SHAPE, analysisRules(language) + SCREENSHOT.empty)

    /**
     * Reads a screenshot without translating it, which is faster and cheaper. It has no rule for screens without
     * text: with one, Mistral Small answered {"lines":[]} for a third of screens full of dialogue.
     */
    val TRANSCRIPTION: String by lazy { prompt(SCREENSHOT, TRANSCRIPTION_SHAPE, emptyList()) }

    /** Translates and explains Japanese text that OCR or a transcription already read. */
    fun forText(language: String): String = prompt(TEXT, ANALYSIS_SHAPE, analysisRules(language) + TEXT.empty)

    fun textRequest(texts: List<String>): String = texts.joinToString("\n")

    /**
     * Translates lines that were already read, without words or grammar notes. The answer is a fraction of a full
     * analysis, so it comes back in a few seconds.
     */
    fun forTranslation(language: String): String = listOf(
        "You help a player read a Japanese video game. You receive the Japanese text read from the game screen, " +
            "as a JSON array of lines.",
        "Answer with one JSON object and nothing else, using exactly this shape:",
        TRANSLATION_SHAPE,
        "",
        "Rules:",
        "- translations: a natural $language translation of each line that suits a game, exactly one per line and " +
            "in the same order. Translate each line in the context of the others.",
    ).joinToString("\n")

    /** The lines as a JSON array, so the model knows exactly how many translations to give. */
    fun translationRequest(texts: List<String>): String = Json.encodeToString(texts)

    /** Where the Japanese text comes from, and how to copy it. */
    private class Source(val intro: String, val lines: String, val japanese: String, val empty: String)

    private val SCREENSHOT = Source(
        intro = "You help a player read a Japanese video game. You receive a screenshot of the game screen.",
        lines = "- lines: every piece of Japanese text the player needs to read (dialogue, narration, speaker names, " +
            "menus, choices, signs), in reading order. Join text that wraps over several rows of the same text box " +
            "into one line. Skip text that is not Japanese, and skip emulator overlays such as FPS counters.",
        // Without this, models swap rare wording for the stock phrase they expect, like 来た for 戻った.
        japanese = "- japanese: the text exactly as shown, character by character, with its punctuation. Copy what is " +
            "written even when a more common phrase would fit: never correct or reword the game's text. " +
            "Do not add spaces.",
        empty = "- If the screen has no Japanese text, answer {\"lines\":[]}.",
    )

    private val TEXT = Source(
        intro = "You help a player read a Japanese video game. You receive the Japanese text read from the game " +
            "screen, one piece of text per line. The text can contain characters that OCR misread.",
        lines = "- lines: one line for each input line that is Japanese, in the same order.",
        japanese = "- japanese: the input line with its punctuation. Fix a character only when it was clearly " +
            "misread, and never reword the game's text. Do not add spaces.",
        empty = "- If there is no Japanese text, answer {\"lines\":[]}.",
    )

    private const val TRANSCRIPTION_SHAPE = "{\"lines\":[{\"japanese\":\"\"}]}"

    private const val TRANSLATION_SHAPE = "{\"translations\":[\"\"]}"

    private const val ANALYSIS_SHAPE = "{\"lines\":[{\"japanese\":\"\",\"translation\":\"\",\"grammar\":\"\"," +
        "\"tokens\":[{\"surface\":\"\",\"reading\":\"\",\"base\":\"\",\"meaning\":\"\",\"pos\":\"\"}]}]}"

    private fun analysisRules(language: String): List<String> = listOf(
        "- translation: a natural $language translation that suits a game.",
        "- grammar: one short $language sentence on the key grammar point or nuance of the line, " +
            "or \"\" when there is nothing notable.",
        "- tokens: the japanese text split into units of meaning, in order. Keep a verb or adjective together " +
            "with all its endings and auxiliaries (食べさせられなかった, 行ってしまった, 見ている), keep compound " +
            "words whole (冒険者), and keep set expressions and grammar patterns whole (かもしれない, " +
            "なければならない, について). Particles are tokens of their own. Joining every surface must give back " +
            "the japanese text exactly, spaces and punctuation included.",
        "- reading: the token's reading in hiragana. Katakana words keep their katakana. " +
            "Use \"\" for punctuation and symbols.",
        "- base: the dictionary form of the token's main word when it is conjugated, otherwise \"\".",
        "- meaning: a short $language gloss of the token in this context, including what its endings add, " +
            "or \"\" for punctuation.",
        "- pos: one of noun, verb, adjective, adverb, particle, auxiliary, pronoun, conjunction, " +
            "interjection, counter, name, expression, punctuation, other.",
    )

    private fun prompt(source: Source, shape: String, rules: List<String>): String = buildList {
        add(source.intro)
        add("Answer with one JSON object and nothing else, using exactly this shape:")
        add(shape)
        add("")
        add("Rules:")
        add(source.lines)
        add(source.japanese)
        addAll(rules)
    }.joinToString("\n")
}
