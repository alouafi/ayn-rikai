package io.github.alouafi.aynrikai.ui.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.alouafi.aynrikai.analysis.ScreenReaderFactory
import io.github.alouafi.aynrikai.appContainer
import io.github.alouafi.aynrikai.llm.ChatRequest
import io.github.alouafi.aynrikai.llm.TruncatedAnswerException
import io.github.alouafi.aynrikai.settings.Settings
import io.github.alouafi.aynrikai.settings.SettingsRepository
import io.github.alouafi.aynrikai.ui.ScanError
import io.github.alouafi.aynrikai.ui.toScanError
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

sealed interface ConnectionCheck {
    data object Idle : ConnectionCheck
    data object Checking : ConnectionCheck
    data object Success : ConnectionCheck
    data class Failure(val error: ScanError) : ConnectionCheck
}

class SettingsViewModel(
    private val repository: SettingsRepository,
    private val readerFactory: ScreenReaderFactory,
) : ViewModel() {

    /** Edited in memory first, so text fields never lag behind the keyboard. */
    var settings by mutableStateOf<Settings?>(null)
        private set

    var connectionCheck by mutableStateOf<ConnectionCheck>(ConnectionCheck.Idle)
        private set

    init {
        viewModelScope.launch { settings = repository.settings.first() }
    }

    fun update(transform: (Settings) -> Settings) {
        val current = settings ?: return
        settings = transform(current)
        connectionCheck = ConnectionCheck.Idle
        viewModelScope.launch { repository.update(transform) }
    }

    /** Sends a tiny request to the configured language model to check the key, URL and model name. */
    fun testConnection() {
        val current = settings ?: return
        connectionCheck = ConnectionCheck.Checking
        viewModelScope.launch {
            connectionCheck = try {
                readerFactory.chatModel(current).complete(PING)
                ConnectionCheck.Success
            } catch (_: TruncatedAnswerException) {
                // The model answered, so the key, URL and model work. Reasoning used up the tiny token budget.
                ConnectionCheck.Success
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                ConnectionCheck.Failure(e.toScanError())
            }
        }
    }

    companion object {
        private val PING = ChatRequest(system = "Reply with the JSON object {\"ok\":true}.", user = "ping", maxTokens = 16)

        val Factory = viewModelFactory {
            initializer {
                val container = checkNotNull(this[APPLICATION_KEY]).appContainer
                SettingsViewModel(container.settingsRepository, container.screenReaderFactory)
            }
        }
    }
}
