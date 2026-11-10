package io.github.alouafi.aynrikai

import android.content.Context
import io.github.alouafi.aynrikai.analysis.ScreenReaderFactory
import io.github.alouafi.aynrikai.capture.CaptureRepository
import io.github.alouafi.aynrikai.dictionary.KanjiDictionary
import io.github.alouafi.aynrikai.dictionary.WordDictionary
import io.github.alouafi.aynrikai.ocr.MlKitJapaneseRecognizer
import io.github.alouafi.aynrikai.settings.SettingsRepository
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/** App-wide singletons, created once in [AynRikaiApp]. */
class AppContainer(context: Context) {

    val settingsRepository = SettingsRepository(context)

    val captureRepository = CaptureRepository()

    val kanjiDictionary = KanjiDictionary { context.assets.open("kanji.tsv") }

    val wordDictionary = WordDictionary(
        openWords = { context.assets.open("words.tsv") },
        openIndex = { context.assets.open("word_index.tsv") },
    )

    // Models send nothing until the whole answer is written, which can take minutes with reasoning on a busy screen.
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(3, TimeUnit.MINUTES)
        .callTimeout(3, TimeUnit.MINUTES)
        .build()

    val screenReaderFactory = ScreenReaderFactory(httpClient, MlKitJapaneseRecognizer())
}
