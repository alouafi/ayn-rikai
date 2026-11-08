package io.github.alouafi.ayntranslate.analysis

/** Prompts that ask a model for an [Analysis] as JSON. `language` is an English language name. */
object AnalysisPrompt {

    const val SCREENSHOT_REQUEST = "Read the Japanese text on this game screen."

    fun forScreenshot(language: String): String = prompt(
        "You help a player read a Japanese video game. You receive a screenshot of the game screen.",
        "- lines: every piece of Japanese text the player needs to read (dialogue, narration, speaker names, " +
            "menus, choices, signs), in reading order. Join text that wraps over several rows of the same text box " +
            "into one line. Skip text that is not Japanese, and skip emulator overlays such as FPS counters.",
        // Without this, models swap rare wording for the stock phrase they expect, like 来た for 戻った.
        "- japanese: the text exactly as shown, character by character, with its punctuation. Copy what is " +
            "written even when a more common phrase would fit: never correct or reword the game's text. " +
            "Do not add spaces.",
        language = language,
        empty = "- If the screen has no Japanese text, answer {\"lines\":[]}.",
    )

    fun forOcrText(language: String): String = prompt(
        "You help a player read a Japanese video game. You receive text that OCR read from the game screen. " +
            "Rows of the same text box are on consecutive lines, and blank lines separate text boxes. " +
            "OCR can misread characters.",
        "- lines: every piece of Japanese text the player needs to read (dialogue, narration, speaker names, " +
            "menus, choices, signs), in reading order. Join rows of the same text box into one line. " +
            "Skip text that is not Japanese.",
        "- japanese: the text as OCR read it, with its punctuation. Fix a character only when OCR clearly " +
            "misread it, and never reword the game's text. Do not add spaces.",
        language = language,
        empty = "- If there is no Japanese text, answer {\"lines\":[]}.",
    )

    fun ocrRequest(blocks: List<String>): String = blocks.joinToString("\n\n")

    private fun prompt(intro: String, vararg sourceRules: String, language: String, empty: String): String =
        buildList {
            add(intro)
            add("Answer with one JSON object and nothing else, using exactly this shape:")
            add(
                "{\"lines\":[{\"japanese\":\"\",\"translation\":\"\",\"grammar\":\"\",\"tokens\":" +
                    "[{\"surface\":\"\",\"reading\":\"\",\"base\":\"\",\"meaning\":\"\",\"pos\":\"\"}]}]}",
            )
            add("")
            add("Rules:")
            addAll(sourceRules)
            add("- translation: a natural $language translation that suits a game.")
            add(
                "- grammar: one short $language sentence on the key grammar point or nuance of the line, " +
                    "or \"\" when there is nothing notable.",
            )
            add(
                "- tokens: the japanese text split into words, in order. Joining every surface must give back " +
                    "the japanese text exactly, spaces and punctuation included.",
            )
            add(
                "- reading: the token's reading in hiragana. Katakana words keep their katakana. " +
                    "Use \"\" for punctuation and symbols.",
            )
            add("- base: the dictionary form when the token is conjugated, otherwise \"\".")
            add("- meaning: a short $language gloss of the token in this context, or \"\" for punctuation.")
            add(
                "- pos: one of noun, verb, adjective, adverb, particle, auxiliary, pronoun, conjunction, " +
                    "interjection, counter, name, expression, punctuation, other.",
            )
            add(empty)
        }.joinToString("\n")
}
