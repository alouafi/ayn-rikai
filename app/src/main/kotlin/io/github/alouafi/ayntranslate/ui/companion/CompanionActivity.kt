package io.github.alouafi.ayntranslate.ui.companion

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import io.github.alouafi.ayntranslate.capture.ScreenCaptureService
import io.github.alouafi.ayntranslate.ui.CaptureConsentActivity
import io.github.alouafi.ayntranslate.ui.settings.SettingsActivity
import io.github.alouafi.ayntranslate.ui.theme.AynTheme
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/** The companion shown on the second screen while the game runs on the first. */
class CompanionActivity : ComponentActivity() {

    private val viewModel: CompanionViewModel by viewModels { CompanionViewModel.Factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state
                    .map { it.settings?.keepControllerOnGame ?: true }
                    .distinctUntilChanged()
                    .collect(::setKeepControllerOnGame)
            }
        }
        setContent {
            AynTheme {
                CompanionScreen(
                    viewModel = viewModel,
                    onStartCapture = { CaptureConsentActivity.start(this) },
                    onStopCapture = { ScreenCaptureService.stop(this) },
                    onOpenSettings = { startActivity(Intent(this, SettingsActivity::class.java)) },
                )
            }
        }
    }

    /**
     * A window that cannot take focus still gets touches, but Android does not move the key focus
     * to this screen when it is touched, so the controller keeps playing the game.
     */
    private fun setKeepControllerOnGame(enabled: Boolean) {
        if (enabled) {
            window.addFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)
        }
    }
}
