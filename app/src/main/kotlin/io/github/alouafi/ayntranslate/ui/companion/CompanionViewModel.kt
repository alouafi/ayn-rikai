package io.github.alouafi.ayntranslate.ui.companion

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.alouafi.ayntranslate.AynTranslateApp
import io.github.alouafi.ayntranslate.analysis.Analysis
import io.github.alouafi.ayntranslate.analysis.BitmapScreenImage
import io.github.alouafi.ayntranslate.analysis.ScreenReaderFactory
import io.github.alouafi.ayntranslate.analysis.Token
import io.github.alouafi.ayntranslate.capture.CaptureRepository
import io.github.alouafi.ayntranslate.capture.CaptureStoppedException
import io.github.alouafi.ayntranslate.capture.NoFrameException
import io.github.alouafi.ayntranslate.capture.SceneChangeDetector
import io.github.alouafi.ayntranslate.capture.crop
import io.github.alouafi.ayntranslate.capture.signature
import io.github.alouafi.ayntranslate.dictionary.Kanji
import io.github.alouafi.ayntranslate.dictionary.KanjiDictionary
import io.github.alouafi.ayntranslate.dictionary.kanjiIn
import io.github.alouafi.ayntranslate.settings.NormalizedRect
import io.github.alouafi.ayntranslate.settings.Settings
import io.github.alouafi.ayntranslate.settings.SettingsRepository
import io.github.alouafi.ayntranslate.ui.ScanError
import io.github.alouafi.ayntranslate.ui.toScanError
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import java.io.IOException

class CompanionViewModel(
    private val settingsRepository: SettingsRepository,
    private val captureRepository: CaptureRepository,
    private val readerFactory: ScreenReaderFactory,
    private val dictionary: KanjiDictionary,
) : ViewModel() {

    private val mutableState = MutableStateFlow(CompanionUiState())
    val state: StateFlow<CompanionUiState> = mutableState.asStateFlow()

    private val scanMutex = Mutex()
    private var autoScanJob: Job? = null
    private var lastFrame: Bitmap? = null
    private var nextResultId = 0L

    init {
        settingsRepository.settings
            .onEach { settings -> mutableState.update { it.copy(settings = settings) } }
            .launchIn(viewModelScope)
        captureRepository.capturer
            .onEach { capturer ->
                val capturing = capturer != null
                mutableState.update {
                    val solved = capturing && it.error == ScanError.NoCapture
                    it.copy(capturing = capturing, error = if (solved) null else it.error)
                }
                if (!capturing) setAutoScan(false)
            }
            .launchIn(viewModelScope)
    }

    fun scan() {
        viewModelScope.launch { scanOnce(detector = null) }
    }

    fun setAutoScan(enabled: Boolean) {
        autoScanJob?.cancel()
        autoScanJob = if (enabled && captureRepository.capturer.value != null) viewModelScope.launch { autoScan() } else null
        mutableState.update { it.copy(autoScan = autoScanJob != null) }
    }

    private suspend fun autoScan() {
        val detector = SceneChangeDetector()
        while (true) {
            scanOnce(detector)
            delay(currentSettings().autoScanIntervalMs)
        }
    }

    /** Captures and reads the screen. With a [detector], reads only when the scene changed and settled. */
    private suspend fun scanOnce(detector: SceneChangeDetector?) {
        if (!scanMutex.tryLock()) return
        val manual = detector == null
        try {
            val capturer = captureRepository.capturer.value
            if (capturer == null) {
                if (manual) showError(ScanError.NoCapture)
                return
            }
            val settings = currentSettings()
            if (manual) mutableState.update { it.copy(reading = true) }
            val frame = capturer.capture().also { lastFrame = it }
            val image = withContext(Dispatchers.Default) { frame.crop(settings.region) }
            if (detector != null) {
                val signature = withContext(Dispatchers.Default) { image.signature() }
                if (!detector.shouldAnalyze(signature)) return
                detector.markAnalyzed(signature)
                mutableState.update { it.copy(reading = true) }
            }
            publish(readerFactory.create(settings).read(BitmapScreenImage(image)))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Auto-scan tries again on its next tick, so a missed frame is not worth a message.
            val missedFrame = e is NoFrameException || e is CaptureStoppedException
            if (manual || !missedFrame) showError(e.toScanError())
        } finally {
            mutableState.update { it.copy(reading = false) }
            scanMutex.unlock()
        }
    }

    private fun publish(analysis: Analysis) {
        if (analysis.lines.isEmpty()) {
            mutableState.update { it.copy(noTextFound = true, error = null) }
            return
        }
        val result = ScanResult(nextResultId++, analysis)
        mutableState.update { state ->
            // Rescanning the same text replaces it instead of stacking duplicates.
            val latest = state.results.firstOrNull()?.analysis
            val sameText = latest?.lines?.map { it.japanese } == analysis.lines.map { it.japanese }
            val older = if (sameText) state.results.drop(1) else state.results
            state.copy(results = (listOf(result) + older).take(MAX_RESULTS), noTextFound = false, error = null)
        }
    }

    fun openWord(token: Token) {
        viewModelScope.launch {
            val kanji = kanjiIn(token.surface + token.base).map { KanjiEntry(it, lookup(it)) }
            pushDetail(Detail.Word(token, kanji))
        }
    }

    fun openKanji(literal: String) {
        viewModelScope.launch { pushDetail(Detail.Character(KanjiEntry(literal, lookup(literal)))) }
    }

    fun closeDetail() {
        mutableState.update { it.copy(details = it.details.dropLast(1)) }
    }

    fun closeAllDetails() {
        mutableState.update { it.copy(details = emptyList()) }
    }

    private fun pushDetail(detail: Detail) {
        mutableState.update { it.copy(details = it.details + detail) }
    }

    private suspend fun lookup(literal: String): Kanji? = try {
        dictionary.lookup(literal)
    } catch (_: IOException) {
        null
    }

    /** Opens the region editor on a fresh frame, or on the last one when capture is off. */
    fun openRegionEditor() {
        viewModelScope.launch {
            val frame = captureOrNull() ?: lastFrame
            if (frame == null) {
                showError(ScanError.NoCapture)
                return@launch
            }
            lastFrame = frame
            mutableState.update { it.copy(regionFrame = frame.asImageBitmap()) }
        }
    }

    private suspend fun captureOrNull(): Bitmap? = try {
        captureRepository.capturer.value?.capture()
    } catch (e: CancellationException) {
        throw e
    } catch (_: Exception) {
        null
    }

    fun saveRegion(region: NormalizedRect?) {
        mutableState.update { it.copy(regionFrame = null) }
        viewModelScope.launch { settingsRepository.update { it.copy(region = region?.takeIf(NormalizedRect::isUsable)) } }
    }

    fun closeRegionEditor() {
        mutableState.update { it.copy(regionFrame = null) }
    }

    fun dismissError() {
        mutableState.update { it.copy(error = null) }
    }

    private fun showError(error: ScanError) {
        mutableState.update { it.copy(error = error, noTextFound = false) }
    }

    private suspend fun currentSettings(): Settings = state.value.settings ?: settingsRepository.settings.first()

    companion object {
        private const val MAX_RESULTS = 20

        val Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as AynTranslateApp).container
                CompanionViewModel(
                    container.settingsRepository,
                    container.captureRepository,
                    container.screenReaderFactory,
                    container.kanjiDictionary,
                )
            }
        }
    }
}
