package io.github.alouafi.aynrikai.ui.settings

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import io.github.alouafi.aynrikai.ui.theme.AynTheme

/**
 * A regular, focusable window: the companion may refuse key focus to keep the controller
 * on the game, but typing an API key needs the keyboard.
 */
class SettingsActivity : ComponentActivity() {

    private val viewModel: SettingsViewModel by viewModels { SettingsViewModel.Factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            AynTheme {
                SettingsScreen(viewModel, onBack = ::finish)
            }
        }
    }
}
