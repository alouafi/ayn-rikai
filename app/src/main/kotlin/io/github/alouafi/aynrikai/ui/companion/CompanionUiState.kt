package io.github.alouafi.aynrikai.ui.companion

import androidx.compose.ui.graphics.ImageBitmap
import io.github.alouafi.aynrikai.analysis.Analysis
import io.github.alouafi.aynrikai.dictionary.Kanji
import io.github.alouafi.aynrikai.settings.Settings
import io.github.alouafi.aynrikai.ui.ScanError

data class CompanionUiState(
    /** Null until the settings are loaded. */
    val settings: Settings? = null,
    val capturing: Boolean = false,
    val autoScan: Boolean = false,
    val reading: Boolean = false,
    val notice: Notice? = null,
    /** Newest first. */
    val results: List<ScanResult> = emptyList(),
    /** The group of characters the player tapped, shown under its line. */
    val lookup: Lookup? = null,
    /** The kanji page shown over the results. */
    val kanjiPage: KanjiEntry? = null,
    /** The frame shown while the reading region is edited, null when the editor is closed. */
    val regionFrame: ImageBitmap? = null,
)

/** What the last scan has to say besides its results. */
sealed interface Notice {
    data object NoTextFound : Notice

    data class Failure(val error: ScanError) : Notice
}

data class ScanResult(val id: Long, val analysis: Analysis, val stage: Stage)

enum class Stage {
    /** The Japanese text only, with a button to translate it. */
    TRANSCRIBED,
    TRANSLATING,
    TRANSLATED,
}

data class KanjiEntry(val literal: String, val kanji: Kanji?)
