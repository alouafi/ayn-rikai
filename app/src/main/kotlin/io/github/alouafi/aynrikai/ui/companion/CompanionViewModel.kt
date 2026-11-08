package io.github.alouafi.aynrikai.ui.companion

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.alouafi.aynrikai.analysis.Analysis
import io.github.alouafi.aynrikai.analysis.Line
import io.github.alouafi.aynrikai.analysis.ScreenReaderFactory
import io.github.alouafi.aynrikai.analysis.withWholeWords
import io.github.alouafi.aynrikai.appContainer
import io.github.alouafi.aynrikai.capture.BitmapScreenImage
import io.github.alouafi.aynrikai.capture.CaptureRepository
import io.github.alouafi.aynrikai.capture.CaptureStoppedException
import io.github.alouafi.aynrikai.capture.FrameUnavailableException
import io.github.alouafi.aynrikai.capture.SceneChangeDetector
import io.github.alouafi.aynrikai.capture.crop
import io.github.alouafi.aynrikai.capture.signature
import io.github.alouafi.aynrikai.dictionary.KanjiDictionary
import io.github.alouafi.aynrikai.dictionary.WordDictionary
import io.github.alouafi.aynrikai.dictionary.containsJapanese
import io.github.alouafi.aynrikai.dictionary.kanjiIn
import io.github.alouafi.aynrikai.settings.Mode
import io.github.alouafi.aynrikai.settings.NormalizedRect
import io.github.alouafi.aynrikai.settings.Settings
import io.github.alouafi.aynrikai.settings.SettingsRepository
import io.github.alouafi.aynrikai.ui.ScanError
import io.github.alouafi.aynrikai.ui.toScanError
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
    private val kanjiDictionary: KanjiDictionary,
    private val wordDictionary: WordDictionary,
) : ViewModel() {

    private val mutableState = MutableStateFlow(CompanionUiState())
    val state: StateFlow<CompanionUiState> = mutableState.asStateFlow()

    private val scanMutex = Mutex()
    private var autoScanJob: Job? = null
    private var lookupJob: Job? = null
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
        // Loads the word dictionary now, so the first tap answers at once.
        viewModelScope.launch {
            try {
                wordDictionary.load()
            } catch (_: IOException) {
                // Lookups catch the same error.
            }
        }
    }

    fun setMode(mode: Mode) {
        viewModelScope.launch { settingsRepository.update { it.copy(mode = mode) } }
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
        val analysis = withWholeWords(readerFactory.create(settings).read(BitmapScreenImage(image)))
        publish(analysis, if (settings.mode == Mode.TRANSCRIBE) Stage.TRANSCRIBED else Stage.TRANSLATED)
    }

    /** The analysis with the words the model split joined back, or as it is when the dictionary cannot load. */
    private suspend fun withWholeWords(analysis: Analysis): Analysis = try {
        analysis.withWholeWords(wordDictionary)
    } catch (_: IOException) {
        analysis
    }

    private fun publish(analysis: Analysis, stage: Stage) {
        if (analysis.lines.isEmpty()) {
            mutableState.update { it.copy(notice = Notice.NoTextFound) }
            return
        }
        val result = ScanResult(nextResultId++, analysis, stage)
        mutableState.update { state ->
            // Rescanning the same text replaces it instead of stacking duplicates, but never undoes a translation.
            val latest = state.results.firstOrNull()
            val sameText = latest?.analysis?.lines?.map { it.japanese } == analysis.lines.map { it.japanese }
            if (sameText && stage == Stage.TRANSCRIBED && latest.stage != Stage.TRANSCRIBED) return@update state.copy(notice = null)
            val results = (listOf(result) + if (sameText) state.results.drop(1) else state.results).take(MAX_RESULTS)
            state.copy(
                results = results,
                notice = null,
                lookup = state.lookup?.takeIf { lookup -> results.any { it.id == lookup.resultId } },
            )
        }
    }

    /** Translates a transcribed result in place. */
    fun translate(resultId: Long) {
        val result = state.value.results.firstOrNull { it.id == resultId && it.stage == Stage.TRANSCRIBED } ?: return
        setStage(resultId, Stage.TRANSLATING)
        viewModelScope.launch {
            val analysis = try {
                withWholeWords(readerFactory.analyzer(currentSettings()).analyze(result.analysis.lines.map { it.japanese }))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                setStage(resultId, Stage.TRANSCRIBED)
                showError(e.toScanError())
                return@launch
            }
            if (analysis.lines.isEmpty()) {
                setStage(resultId, Stage.TRANSCRIBED)
                showError(ScanError.Unreadable)
                return@launch
            }
            mutableState.update { state ->
                state.copy(
                    results = state.results.map { if (it.id == resultId) it.copy(analysis = analysis, stage = Stage.TRANSLATED) else it },
                    lookup = state.lookup?.takeUnless { it.resultId == resultId },
                )
            }
        }
    }

    private fun setStage(resultId: Long, stage: Stage) {
        mutableState.update { state ->
            state.copy(results = state.results.map { if (it.id == resultId) it.copy(stage = stage) else it })
        }
    }

    /** Shows the group of characters at [offset] of a line, or hides it when it is tapped again. */
    fun lookUp(resultId: Long, lineIndex: Int, offset: Int) {
        lookupJob?.cancel()
        if (state.value.lookup?.covers(resultId, lineIndex, offset) == true) {
            closeLookup()
            return
        }
        val line = state.value.results.firstOrNull { it.id == resultId }?.analysis?.lines?.getOrNull(lineIndex) ?: return
        lookupJob = viewModelScope.launch {
            val lookup = try {
                findLookup(resultId, lineIndex, line, offset)
            } catch (_: IOException) {
                null
            }
            mutableState.update { it.copy(lookup = lookup) }
        }
    }

    /** The model's token at [offset] on lines it split, the longest dictionary word elsewhere. */
    private suspend fun findLookup(resultId: Long, lineIndex: Int, line: Line, offset: Int): Lookup? {
        val surface = line.surface
        if (offset !in surface.indices) return null
        val tokenIndex = line.tokenRanges().indexOfFirst { offset in it }
        val token = line.tokens.getOrNull(tokenIndex)
        if (token != null && !token.isWord) return null
        val range = if (token != null) line.tokenRanges()[tokenIndex] else wordDictionary.wordAt(surface, offset)
        val text = surface.substring(range)
        if (!containsJapanese(text)) return null
        return Lookup(
            resultId = resultId,
            line = lineIndex,
            range = range,
            text = text,
            token = token,
            words = wordDictionary.search(text).take(MAX_WORDS),
            kanji = kanjiIn(text + token?.base.orEmpty()).map { KanjiEntry(it, kanjiDictionary.lookup(it)) },
        )
    }

    fun closeLookup() {
        lookupJob?.cancel()
        mutableState.update { it.copy(lookup = null) }
    }

    fun openKanji(entry: KanjiEntry) {
        mutableState.update { it.copy(kanjiPage = entry) }
    }

    fun closeKanji() {
        mutableState.update { it.copy(kanjiPage = null) }
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

        /** Dictionary words shown for a tapped group: the full match, then the shorter words it starts with. */
        private const val MAX_WORDS = 4

        val Factory = viewModelFactory {
            initializer {
                val container = checkNotNull(this[APPLICATION_KEY]).appContainer
                CompanionViewModel(
                    container.settingsRepository,
                    container.captureRepository,
                    container.screenReaderFactory,
                    container.kanjiDictionary,
                    container.wordDictionary,
                )
            }
        }
    }
}
