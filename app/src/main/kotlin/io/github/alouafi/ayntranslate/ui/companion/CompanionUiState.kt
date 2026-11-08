package io.github.alouafi.ayntranslate.ui.companion

import androidx.compose.ui.graphics.ImageBitmap
import io.github.alouafi.ayntranslate.analysis.Analysis
import io.github.alouafi.ayntranslate.analysis.Token
import io.github.alouafi.ayntranslate.dictionary.Kanji
import io.github.alouafi.ayntranslate.settings.Settings
import io.github.alouafi.ayntranslate.ui.ScanError

data class CompanionUiState(
    /** Null until the settings are loaded. */
    val settings: Settings? = null,
    val capturing: Boolean = false,
    val autoScan: Boolean = false,
    val reading: Boolean = false,
    val error: ScanError? = null,
    val noTextFound: Boolean = false,
    /** Newest first. */
    val results: List<ScanResult> = emptyList(),
    /** Dictionary pages, the last one on top. */
    val details: List<Detail> = emptyList(),
    /** The frame shown while the reading region is edited, null when the editor is closed. */
    val regionFrame: ImageBitmap? = null,
)

data class ScanResult(val id: Long, val analysis: Analysis)

data class KanjiEntry(val literal: String, val kanji: Kanji?)

sealed interface Detail {
    data class Word(val token: Token, val kanji: List<KanjiEntry>) : Detail

    data class Character(val entry: KanjiEntry) : Detail
}
