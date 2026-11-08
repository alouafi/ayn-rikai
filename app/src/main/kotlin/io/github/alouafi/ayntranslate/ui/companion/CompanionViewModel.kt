package io.github.alouafi.ayntranslate.ui.companion

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.alouafi.ayntranslate.analysis.Analysis
import io.github.alouafi.ayntranslate.analysis.ScreenReaderFactory
import io.github.alouafi.ayntranslate.analysis.Token
import io.github.alouafi.ayntranslate.appContainer
import io.github.alouafi.ayntranslate.capture.BitmapScreenImage
import io.github.alouafi.ayntranslate.capture.CaptureRepository
import io.github.alouafi.ayntranslate.capture.CaptureStoppedException
import io.github.alouafi.ayntranslate.capture.FrameUnavailableException
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
                    val solved = capturing && it.notice == Notice.Failure(ScanError.NoCapture)
                    it.copy(capturing = capturing, notice = if (solved) null else it.notice)
                }
                if (!capturing) setAutoScan(false)
            }
            .launchIn(viewModelScope)
    }

    fun scan() {
        viewModelScope.launch { exclusiveScan { settings -> whileReading { read(capture(settings), settings) } } }
    }

    fun setAutoScan(enabled: Boolean) {
        autoScanJob?.cancel()
        autoScanJob = if (enabled && captureRepository.capturer.value != null) viewModelScope.launch { autoScan() } else null
        mutableState.update { it.copy(autoScan = autoScanJob != null) }
    }

    /** Reads the screen whenever the picture shows a new scene that has settled. */
    private suspend fun autoScan() {
        val detector = SceneChangeDetector()
        while (true) {
            exclusiveScan { settings ->
                // Auto-scan tries again on its next tick, so a missed frame is not worth a message.
                val image = try {
                    capture(settings)
                } catch (_: FrameUnavailableException) {
                    return@exclusiveScan
                }
                val signature = withContext(Dispatchers.Default) { image.signature() }
                if (detector.shouldAnalyze(signature)) whileReading { read(image, settings) }
            }
            delay(currentSettings().autoScanIntervalMs)
        }
    }

    /** Runs [scan] unless another scan is running, and shows what goes wrong. */
    private suspend fun exclusiveScan(scan: suspend (Settings) -> Unit) {
        if (!scanMutex.tryLock()) return
        try {
            scan(currentSettings())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            showError(e.toScanError())
        } finally {
            scanMutex.unlock()
        }
    }

    /** The reading region of a fresh frame. */
    private suspend fun capture(settings: Settings): Bitmap {
        val capturer = captureRepository.capturer.value ?: throw CaptureStoppedException()
        val frame = capturer.capture().also { lastFrame = it }
        return withContext(Dispatchers.Default) { frame.crop(settings.region) }
    }

    private suspend fun whileReading(block: suspend () -> Unit) {
        mutableState.update { it.copy(reading = true) }
        try {
            block()
        } finally {
            mutableState.update { it.copy(reading = false) }
        }
    }

    private suspend fun read(image: Bitmap, settings: Settings) {
        publish(readerFactory.create(settings).read(BitmapScreenImage(image)))
    }

    private fun publish(analysis: Analysis) {
        if (analysis.lines.isEmpty()) {
            mutableState.update { it.copy(notice = Notice.NoTextFound) }
            return
        }
        val result = ScanResult(nextResultId++, analysis)
        mutableState.update { state ->
            // Rescanning the same text replaces it instead of stacking duplicates.
            val latest = state.results.firstOrNull()?.analysis
            val sameText = latest?.lines?.map { it.japanese } == analysis.lines.map { it.japanese }
            val older = if (sameText) state.results.drop(1) else state.results
            state.copy(results = (listOf(result) + older).take(MAX_RESULTS), notice = null)
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

    fun dismissNotice() {
        mutableState.update { it.copy(notice = null) }
    }

    private fun showError(error: ScanError) {
        mutableState.update { it.copy(notice = Notice.Failure(error)) }
    }

    private suspend fun currentSettings(): Settings = state.value.settings ?: settingsRepository.settings.first()

    companion object {
        private const val MAX_RESULTS = 20

        val Factory = viewModelFactory {
            initializer {
                val container = checkNotNull(this[APPLICATION_KEY]).appContainer
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
