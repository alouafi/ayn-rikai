package io.github.alouafi.aynrikai.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.settingsStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(context: Context, private val cipher: SecretCipher = SecretCipher()) {

    private val store = context.applicationContext.settingsStore

    val settings: Flow<Settings> = store.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map(::read)
        .distinctUntilChanged()

    /** Applies [transform] to the stored settings, so concurrent screens never overwrite each other's fields. */
    suspend fun update(transform: (Settings) -> Settings) {
        store.edit { preferences -> write(preferences, transform(read(preferences))) }
    }

    private fun read(preferences: Preferences): Settings {
        val defaults = Settings()
        return Settings(
            mode = enumValue(preferences[MODE], defaults.mode),
            textSource = enumValue(preferences[TEXT_SOURCE], defaults.textSource),
            provider = enumValue(preferences[PROVIDER], defaults.provider),
            mistralApiKey = cipher.decrypt(preferences[MISTRAL_API_KEY].orEmpty()),
            mistralModel = preferences[MISTRAL_MODEL] ?: defaults.mistralModel,
            mistralReasoning = preferences[MISTRAL_REASONING] ?: defaults.mistralReasoning,
            openAiBaseUrl = preferences[OPENAI_BASE_URL] ?: defaults.openAiBaseUrl,
            openAiApiKey = cipher.decrypt(preferences[OPENAI_API_KEY].orEmpty()),
            openAiModel = preferences[OPENAI_MODEL] ?: defaults.openAiModel,
            openAiReasoningEffort = enumValue(preferences[OPENAI_REASONING_EFFORT], defaults.openAiReasoningEffort),
            targetLanguage = preferences[TARGET_LANGUAGE] ?: defaults.targetLanguage,
            showFurigana = preferences[SHOW_FURIGANA] ?: defaults.showFurigana,
            showRomaji = preferences[SHOW_ROMAJI] ?: defaults.showRomaji,
            autoScanIntervalMs = (preferences[AUTO_SCAN_INTERVAL_MS] ?: defaults.autoScanIntervalMs)
                .coerceIn(Settings.AUTO_SCAN_INTERVAL_RANGE_MS),
            keepControllerOnGame = preferences[KEEP_CONTROLLER_ON_GAME] ?: defaults.keepControllerOnGame,
            region = preferences[REGION]?.let(NormalizedRect::decode),
        )
    }

    private fun write(preferences: MutablePreferences, settings: Settings) {
        preferences[MODE] = settings.mode.name
        preferences[TEXT_SOURCE] = settings.textSource.name
        preferences[PROVIDER] = settings.provider.name
        preferences[MISTRAL_API_KEY] = cipher.encrypt(settings.mistralApiKey)
        preferences[MISTRAL_MODEL] = settings.mistralModel
        preferences[MISTRAL_REASONING] = settings.mistralReasoning
        preferences[OPENAI_BASE_URL] = settings.openAiBaseUrl
        preferences[OPENAI_API_KEY] = cipher.encrypt(settings.openAiApiKey)
        preferences[OPENAI_MODEL] = settings.openAiModel
        preferences[OPENAI_REASONING_EFFORT] = settings.openAiReasoningEffort.name
        preferences[TARGET_LANGUAGE] = settings.targetLanguage
        preferences[SHOW_FURIGANA] = settings.showFurigana
        preferences[SHOW_ROMAJI] = settings.showRomaji
        preferences[AUTO_SCAN_INTERVAL_MS] = settings.autoScanIntervalMs
        preferences[KEEP_CONTROLLER_ON_GAME] = settings.keepControllerOnGame
        val region = settings.region
        if (region == null) preferences.remove(REGION) else preferences[REGION] = region.encode()
    }

    private inline fun <reified T : Enum<T>> enumValue(name: String?, default: T): T =
        enumValues<T>().firstOrNull { it.name == name } ?: default

    private companion object {
        val MODE = stringPreferencesKey("mode")
        val TEXT_SOURCE = stringPreferencesKey("text_source")
        val PROVIDER = stringPreferencesKey("provider")
        val MISTRAL_API_KEY = stringPreferencesKey("mistral_api_key")
        val MISTRAL_MODEL = stringPreferencesKey("mistral_model")
        val MISTRAL_REASONING = booleanPreferencesKey("mistral_reasoning")
        val OPENAI_BASE_URL = stringPreferencesKey("openai_base_url")
        val OPENAI_API_KEY = stringPreferencesKey("openai_api_key")
        val OPENAI_MODEL = stringPreferencesKey("openai_model")
        val OPENAI_REASONING_EFFORT = stringPreferencesKey("openai_reasoning_effort")
        val TARGET_LANGUAGE = stringPreferencesKey("target_language")
        val SHOW_FURIGANA = booleanPreferencesKey("show_furigana")
        val SHOW_ROMAJI = booleanPreferencesKey("show_romaji")
        val AUTO_SCAN_INTERVAL_MS = longPreferencesKey("auto_scan_interval_ms")
        val KEEP_CONTROLLER_ON_GAME = booleanPreferencesKey("keep_controller_on_game")
        val REGION = stringPreferencesKey("region")
    }
}
