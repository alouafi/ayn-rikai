package io.github.alouafi.aynrikai.ui.companion

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.alouafi.aynrikai.R
import io.github.alouafi.aynrikai.settings.Mode
import io.github.alouafi.aynrikai.settings.Settings
import io.github.alouafi.aynrikai.ui.ScanError
import io.github.alouafi.aynrikai.ui.text

@Composable
fun CompanionScreen(
    viewModel: CompanionViewModel,
    onStartCapture: () -> Unit,
    onStopCapture: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val settings = state.settings ?: return

    val regionFrame = state.regionFrame
    if (regionFrame != null) {
        BackHandler(onBack = viewModel::closeRegionEditor)
        RegionEditor(
            frame = regionFrame,
            initial = settings.region,
            onSave = viewModel::saveRegion,
            onCancel = viewModel::closeRegionEditor,
        )
        return
    }

    // Registered last, the kanji page closes before the lookup under it.
    BackHandler(enabled = state.lookup != null, onBack = viewModel::closeLookup)
    BackHandler(enabled = state.kanjiPage != null, onBack = viewModel::closeKanji)
    Scaffold(
        topBar = {
            TopBar(
                state = state,
                onStopCapture = onStopCapture,
                onAutoScanChange = viewModel::setAutoScan,
                onEditRegion = viewModel::openRegionEditor,
                onOpenSettings = onOpenSettings,
            )
        },
        bottomBar = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth().padding(12.dp),
            ) {
                ModeSwitch(mode = settings.mode, onModeChange = viewModel::setMode)
                ScanButton(
                    capturing = state.capturing,
                    reading = state.reading,
                    onStartCapture = onStartCapture,
                    onScan = viewModel::scan,
                    modifier = Modifier.weight(1f),
                )
            }
        },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            Results(
                state = state,
                settings = settings,
                onTap = viewModel::lookUp,
                onKanji = viewModel::openKanji,
                onTranslate = viewModel::translate,
                onDismissNotice = viewModel::dismissNotice,
                onOpenSettings = onOpenSettings,
            )
            // Over the results rather than instead of them, so they keep their scroll position.
            state.kanjiPage?.let { entry ->
                Surface(Modifier.fillMaxSize()) {
                    KanjiPanel(entry, settings.targetLanguage, onBack = viewModel::closeKanji)
                }
            }
        }
    }
}

@Composable
private fun TopBar(
    state: CompanionUiState,
    onStopCapture: () -> Unit,
    onAutoScanChange: (Boolean) -> Unit,
    onEditRegion: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.fillMaxWidth().padding(start = 12.dp, end = 4.dp, top = 4.dp),
    ) {
        if (state.capturing) {
            AssistChip(
                onClick = onStopCapture,
                label = { Text(stringResource(R.string.capture_on)) },
                trailingIcon = {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = stringResource(R.string.action_stop_capture),
                        modifier = Modifier.size(AssistChipDefaults.IconSize),
                    )
                },
            )
            FilterChip(
                selected = state.autoScan,
                onClick = { onAutoScanChange(!state.autoScan) },
                label = { Text(stringResource(R.string.action_auto)) },
                leadingIcon = { Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp)) },
            )
        } else {
            Text(
                stringResource(R.string.capture_off),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.weight(1f))
        IconButton(onClick = onEditRegion) {
            Icon(painterResource(R.drawable.ic_crop), contentDescription = stringResource(R.string.action_region))
        }
        IconButton(onClick = onOpenSettings) {
            Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.action_settings))
        }
    }
}

/** Transcribe shows the Japanese alone, with a button to translate it; Translate does both in one go. */
@Composable
private fun ModeSwitch(mode: Mode, onModeChange: (Mode) -> Unit) {
    SingleChoiceSegmentedButtonRow {
        Mode.entries.forEachIndexed { index, option ->
            SegmentedButton(
                selected = option == mode,
                onClick = { onModeChange(option) },
                shape = SegmentedButtonDefaults.itemShape(index, Mode.entries.size),
            ) {
                Text(stringResource(option.label))
            }
        }
    }
}

private val Mode.label: Int
    get() = when (this) {
        Mode.TRANSCRIBE -> R.string.mode_transcribe
        Mode.TRANSLATE -> R.string.mode_translate
    }

@Composable
private fun ScanButton(
    capturing: Boolean,
    reading: Boolean,
    onStartCapture: () -> Unit,
    onScan: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = if (capturing) onScan else onStartCapture,
        enabled = !reading,
        modifier = modifier.height(56.dp),
    ) {
        when {
            reading -> {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(12.dp))
                Text(stringResource(R.string.status_reading))
            }
            capturing -> {
                Icon(Icons.Default.Search, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.action_scan))
            }
            else -> Text(stringResource(R.string.action_start_capture))
        }
    }
}

@Composable
private fun Results(
    state: CompanionUiState,
    settings: Settings,
    onTap: (resultId: Long, line: Int, offset: Int) -> Unit,
    onKanji: (KanjiEntry) -> Unit,
    onTranslate: (resultId: Long) -> Unit,
    onDismissNotice: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val listState = rememberLazyListState()
    val newest = state.results.firstOrNull()?.id
    LaunchedEffect(newest) {
        if (newest != null) listState.animateScrollToItem(0)
    }
    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        when (val notice = state.notice) {
            null -> Unit
            Notice.NoTextFound -> item(key = "no-text") { Hint(stringResource(R.string.no_text_found)) }
            is Notice.Failure -> item(key = "error") {
                ErrorCard(notice.error, onDismiss = onDismissNotice, onOpenSettings = onOpenSettings)
            }
        }
        if (state.results.isEmpty()) {
            item(key = "empty") { EmptyState(state.capturing) }
        }
        state.results.forEachIndexed { index, result ->
            if (index == 1) {
                item(key = "earlier") { Hint(stringResource(R.string.earlier)) }
            }
            itemsIndexed(result.analysis.lines, key = { lineIndex, _ -> "${result.id}-$lineIndex" }) { lineIndex, line ->
                LineCard(
                    line = line,
                    settings = settings,
                    dimmed = index > 0,
                    lookup = state.lookup?.takeIf { it.resultId == result.id && it.line == lineIndex },
                    onTap = { offset -> onTap(result.id, lineIndex, offset) },
                    onKanji = onKanji,
                )
            }
            if (result.stage != Stage.TRANSLATED) {
                item(key = "${result.id}-translate") {
                    TranslateButton(translating = result.stage == Stage.TRANSLATING, onClick = { onTranslate(result.id) })
                }
            }
        }
    }
}

@Composable
private fun TranslateButton(translating: Boolean, onClick: () -> Unit) {
    FilledTonalButton(onClick = onClick, enabled = !translating, modifier = Modifier.fillMaxWidth()) {
        if (translating) {
            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
            Spacer(Modifier.width(12.dp))
            Text(stringResource(R.string.status_translating))
        } else {
            Text(stringResource(R.string.action_translate))
        }
    }
}

@Composable
private fun EmptyState(capturing: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 24.dp, horizontal = 8.dp)) {
        if (!capturing) {
            Text(stringResource(R.string.empty_title), style = MaterialTheme.typography.titleLarge)
        }
        Text(
            stringResource(if (capturing) R.string.empty_ready else R.string.empty_body),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun Hint(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
    )
}

@Composable
private fun ErrorCard(error: ScanError, onDismiss: () -> Unit, onOpenSettings: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
        Column(Modifier.padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 4.dp)) {
            Text(error.text(), color = MaterialTheme.colorScheme.onErrorContainer)
            Row(Modifier.align(Alignment.End)) {
                if (error.concernsSettings) {
                    TextButton(onClick = onOpenSettings) { Text(stringResource(R.string.action_open_settings)) }
                }
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_dismiss)) }
            }
        }
    }
}
