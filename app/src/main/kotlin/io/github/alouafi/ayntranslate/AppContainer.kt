package io.github.alouafi.ayntranslate

import android.content.Context
import io.github.alouafi.ayntranslate.analysis.ScreenReaderFactory
import io.github.alouafi.ayntranslate.capture.CaptureRepository
import io.github.alouafi.ayntranslate.dictionary.KanjiDictionary
import io.github.alouafi.ayntranslate.ocr.MlKitJapaneseRecognizer
import io.github.alouafi.ayntranslate.settings.SettingsRepository
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/** App-wide singletons, created once in [AynTranslateApp]. */
class AppContainer(context: Context) {

    val settingsRepository = SettingsRepository(context)

    val captureRepository = CaptureRepository()

    val kanjiDictionary = KanjiDictionary { context.assets.open("kanji.tsv") }

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .callTimeout(2, TimeUnit.MINUTES)
        .build()

    val screenReaderFactory = ScreenReaderFactory(httpClient, MlKitJapaneseRecognizer())
}
