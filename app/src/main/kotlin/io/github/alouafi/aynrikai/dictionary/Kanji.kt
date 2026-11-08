package io.github.alouafi.aynrikai.dictionary

/** One KANJIDIC2 entry. */
data class Kanji(
    val literal: String,
    val strokes: Int,
    /** 1-6 for kyōiku grades, 8 for other jōyō kanji, 9-10 for jinmeiyō (names), null otherwise. */
    val grade: Int?,
    /** Rank among the 2,500 most used kanji in newspapers, null when rarer. */
    val frequency: Int?,
    /** Kangxi radical number, 1-214. */
    val radical: Int?,
    val onReadings: List<String>,
    /** Kun readings, okurigana in brackets: かた（る）. `-` marks a prefix or suffix. */
    val kunReadings: List<String>,
    val meaningsEn: List<String>,
    val meaningsFr: List<String>,
) {
    val radicalSymbol: String?
        get() = radical?.takeIf { it in 1..RADICAL_COUNT }?.let { (KANGXI_RADICALS_START + it - 1).toChar().toString() }

    /** Meanings in the requested language when KANJIDIC2 has them, English otherwise. */
    fun meanings(languageTag: String): List<String> =
        if (languageTag.startsWith("fr") && meaningsFr.isNotEmpty()) meaningsFr else meaningsEn

    private companion object {
        const val KANGXI_RADICALS_START = 0x2F00
        const val RADICAL_COUNT = 214
    }
}
