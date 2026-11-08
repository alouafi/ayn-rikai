package io.github.alouafi.ayntranslate.ui.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import io.github.alouafi.ayntranslate.BuildConfig
import io.github.alouafi.ayntranslate.R
import io.github.alouafi.ayntranslate.settings.OcrEngine
import io.github.alouafi.ayntranslate.settings.Pipeline
import io.github.alouafi.ayntranslate.settings.Provider
import io.github.alouafi.ayntranslate.settings.Settings
import io.github.alouafi.ayntranslate.ui.text
import java.util.Locale
import kotlin.math.roundToLong

@Composable
fun SettingsScreen(viewModel: SettingsViewModel, onBack: () -> Unit) {
    val settings = viewModel.settings ?: return
    val update = viewModel::update

    Column(Modifier.fillMaxSize().safeDrawingPadding()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(4.dp)) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
            }
            Text(stringResource(R.string.settings_title), style = MaterialTheme.typography.titleLarge)
        }
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, bottom = 24.dp),
        ) {
            ReadingSection(settings, update)
            if (settings.needsMistralKey) {
                MistralSection(settings, update)
            }
            if (settings.uses(Provider.OPENAI_COMPATIBLE)) {
                OpenAiSection(settings, update)
            }
            if (settings.usesLanguageModel) {
                ConnectionTest(viewModel.connectionCheck, onTest = viewModel::testConnection)
                LanguageSection(settings, update)
            }
            DisplaySection(settings, update)
            AboutSection()
        }
    }
}

private typealias SettingsUpdate = ((Settings) -> Settings) -> Unit

@Composable
private fun ReadingSection(settings: Settings, update: SettingsUpdate) {
    SectionTitle(R.string.settings_pipeline)
    ChoiceGroup(settings.pipeline, PIPELINES) { pipeline -> update { it.copy(pipeline = pipeline) } }
    if (settings.usesOcr) {
        SectionTitle(R.string.settings_ocr)
        ChoiceGroup(settings.ocrEngine, OCR_ENGINES) { engine -> update { it.copy(ocrEngine = engine) } }
    }
    if (settings.usesLanguageModel) {
        SectionTitle(R.string.settings_provider)
        ChoiceGroup(settings.provider, PROVIDERS) { provider -> update { it.copy(provider = provider) } }
    }
}

private class Option<T>(val value: T, @param:StringRes val title: Int, @param:StringRes val description: Int)

private val PIPELINES = listOf(
    Option(Pipeline.VISION, R.string.pipeline_vision, R.string.pipeline_vision_description),
    Option(Pipeline.OCR_THEN_LLM, R.string.pipeline_ocr_llm, R.string.pipeline_ocr_llm_description),
    Option(Pipeline.OCR_ONLY, R.string.pipeline_ocr_only, R.string.pipeline_ocr_only_description),
)

private val OCR_ENGINES = listOf(
    Option(OcrEngine.ON_DEVICE, R.string.ocr_on_device, R.string.ocr_on_device_description),
    Option(OcrEngine.MISTRAL, R.string.ocr_mistral, R.string.ocr_mistral_description),
)

private val PROVIDERS = listOf(
    Option(Provider.MISTRAL, R.string.provider_mistral, R.string.provider_mistral_description),
    Option(Provider.OPENAI_COMPATIBLE, R.string.provider_openai, R.string.provider_openai_description),
)

@Composable
private fun MistralSection(settings: Settings, update: SettingsUpdate) {
    SectionTitle(R.string.settings_mistral)
    SecretField(
        label = stringResource(R.string.field_api_key),
        value = settings.mistralApiKey,
        onValueChange = { key -> update { it.copy(mistralApiKey = key) } },
        hint = stringResource(R.string.field_mistral_key_hint),
    )
    if (settings.uses(Provider.MISTRAL)) {
        TextField(
            label = stringResource(R.string.field_model),
            value = settings.mistralModel,
            onValueChange = { model -> update { it.copy(mistralModel = model) } },
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = settings.mistralModel == Settings.DEFAULT_MISTRAL_MODEL,
                onClick = { update { it.copy(mistralModel = Settings.DEFAULT_MISTRAL_MODEL) } },
                label = { Text(stringResource(R.string.model_small)) },
            )
            FilterChip(
                selected = settings.mistralModel == Settings.MEDIUM_MISTRAL_MODEL,
                onClick = { update { it.copy(mistralModel = Settings.MEDIUM_MISTRAL_MODEL) } },
                label = { Text(stringResource(R.string.model_medium)) },
            )
        }
    }
}

@Composable
private fun OpenAiSection(settings: Settings, update: SettingsUpdate) {
    SectionTitle(R.string.settings_openai)
    TextField(
        label = stringResource(R.string.field_base_url),
        value = settings.openAiBaseUrl,
        onValueChange = { url -> update { it.copy(openAiBaseUrl = url) } },
        hint = stringResource(R.string.field_base_url_hint),
        keyboardType = KeyboardType.Uri,
    )
    SecretField(
        label = stringResource(R.string.field_api_key_optional),
        value = settings.openAiApiKey,
        onValueChange = { key -> update { it.copy(openAiApiKey = key) } },
    )
    TextField(
        label = stringResource(R.string.field_model),
        value = settings.openAiModel,
        onValueChange = { model -> update { it.copy(openAiModel = model) } },
        hint = stringResource(
            if (settings.pipeline == Pipeline.VISION) R.string.field_model_vision_hint else R.string.field_model_text_hint,
        ),
    )
}

@Composable
private fun ConnectionTest(check: ConnectionCheck, onTest: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.padding(top = 4.dp),
    ) {
        OutlinedButton(onClick = onTest, enabled = check != ConnectionCheck.Checking) {
            Text(stringResource(R.string.action_test_connection))
        }
        when (check) {
            ConnectionCheck.Idle -> Unit
            ConnectionCheck.Checking -> CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
            ConnectionCheck.Success -> Text(
                stringResource(R.string.connection_ok),
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodyMedium,
            )
            is ConnectionCheck.Failure -> Unit
        }
    }
    if (check is ConnectionCheck.Failure) {
        Text(check.error.text(), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun LanguageSection(settings: Settings, update: SettingsUpdate) {
    SectionTitle(R.string.settings_language)
    val locale = LocalLocale.current.platformLocale
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Settings.LANGUAGES.forEach { tag ->
            FilterChip(
                selected = settings.targetLanguage == tag,
                onClick = { update { it.copy(targetLanguage = tag) } },
                label = { Text(languageName(tag, locale)) },
            )
        }
    }
}

@Composable
private fun DisplaySection(settings: Settings, update: SettingsUpdate) {
    SectionTitle(R.string.settings_display)
    Toggle(
        title = stringResource(R.string.setting_furigana),
        description = stringResource(R.string.setting_furigana_description),
        checked = settings.showFurigana,
        onCheckedChange = { checked -> update { it.copy(showFurigana = checked) } },
    )
    Toggle(
        title = stringResource(R.string.setting_romaji),
        description = stringResource(R.string.setting_romaji_description),
        checked = settings.showRomaji,
        onCheckedChange = { checked -> update { it.copy(showRomaji = checked) } },
    )
    Toggle(
        title = stringResource(R.string.setting_keep_controller),
        description = stringResource(R.string.setting_keep_controller_description),
        checked = settings.keepControllerOnGame,
        onCheckedChange = { checked -> update { it.copy(keepControllerOnGame = checked) } },
    )
    AutoScanInterval(settings.autoScanIntervalMs) { interval -> update { it.copy(autoScanIntervalMs = interval) } }
}

@Composable
private fun AutoScanInterval(intervalMs: Long, onChange: (Long) -> Unit) {
    val range = Settings.AUTO_SCAN_INTERVAL_RANGE_MS
    var seconds by remember(intervalMs) { mutableFloatStateOf(intervalMs / MS_PER_SECOND) }
    Column(Modifier.padding(vertical = 8.dp)) {
        Text(stringResource(R.string.setting_auto_scan_interval), style = MaterialTheme.typography.bodyLarge)
        Text(
            stringResource(R.string.setting_auto_scan_interval_value, seconds),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Slider(
            value = seconds,
            onValueChange = { seconds = it },
            valueRange = range.first / MS_PER_SECOND..range.last / MS_PER_SECOND,
            // Half-second steps.
            steps = ((range.last - range.first) / HALF_SECOND_MS - 1).toInt(),
            onValueChangeFinished = { onChange((seconds * MS_PER_SECOND).roundToLong()) },
        )
    }
}

@Composable
private fun AboutSection() {
    val uriHandler = LocalUriHandler.current
    SectionTitle(R.string.settings_about)
    Text(
        stringResource(R.string.about_version, BuildConfig.VERSION_NAME),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Text(
        stringResource(R.string.about_kanjidic),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        TextButton(onClick = { uriHandler.openUri(SOURCE_URL) }) { Text(stringResource(R.string.about_source)) }
        TextButton(onClick = { uriHandler.openUri(KANJIDIC_LICENSE_URL) }) { Text(stringResource(R.string.about_kanjidic_license)) }
    }
}

@Composable
private fun SectionTitle(@StringRes title: Int) {
    Column(Modifier.padding(top = 16.dp)) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Text(
            stringResource(title),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 16.dp, bottom = 4.dp),
        )
    }
}

@Composable
private fun <T> ChoiceGroup(selected: T, options: List<Option<T>>, onSelect: (T) -> Unit) {
    Column(Modifier.selectableGroup()) {
        options.forEach { option ->
            Choice(
                title = stringResource(option.title),
                description = stringResource(option.description),
                selected = selected == option.value,
                onClick = { onSelect(option.value) },
            )
        }
    }
}

@Composable
private fun Choice(title: String, description: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton)
            .padding(vertical = 6.dp),
    ) {
        RadioButton(selected = selected, onClick = null)
        Spacer(Modifier.width(12.dp))
        Labels(title, description, Modifier.weight(1f))
    }
}

@Composable
private fun Toggle(title: String, description: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(value = checked, onValueChange = onCheckedChange, role = Role.Switch)
            .padding(vertical = 8.dp),
    ) {
        Labels(title, description, Modifier.weight(1f))
        Spacer(Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = null)
    }
}

@Composable
private fun Labels(title: String, description: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(title, style = MaterialTheme.typography.bodyLarge)
        Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun TextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    hint: String? = null,
    keyboardType: KeyboardType = KeyboardType.Ascii,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        supportingText = hint?.let { { Text(it) } },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, autoCorrectEnabled = false),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun SecretField(label: String, value: String, onValueChange: (String) -> Unit, hint: String? = null) {
    var visible by rememberSaveable { mutableStateOf(false) }
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        supportingText = hint?.let { { Text(it) } },
        singleLine = true,
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, autoCorrectEnabled = false),
        trailingIcon = {
            TextButton(onClick = { visible = !visible }) {
                Text(stringResource(if (visible) R.string.action_hide else R.string.action_show))
            }
        },
        modifier = Modifier.fillMaxWidth(),
    )
}

/** The language name in the interface language: "Anglais" in French, "English" in English. */
private fun languageName(tag: String, displayLocale: Locale): String =
    Locale.forLanguageTag(tag).getDisplayLanguage(displayLocale).replaceFirstChar { it.titlecase(displayLocale) }

private const val MS_PER_SECOND = 1_000f
private const val HALF_SECOND_MS = 500L
private const val SOURCE_URL = "https://github.com/alouafi/ayn-translate"
private const val KANJIDIC_LICENSE_URL = "https://www.edrdg.org/edrdg/licence.html"
