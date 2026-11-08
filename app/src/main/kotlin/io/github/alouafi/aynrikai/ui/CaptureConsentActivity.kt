package io.github.alouafi.aynrikai.ui

import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionConfig
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.view.Display
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import io.github.alouafi.aynrikai.capture.ScreenCaptureService
import io.github.alouafi.aynrikai.display.startActivityOnDisplay

/**
 * Asks for the screen capture consent from the game screen, then disappears.
 *
 * The system consent dialog closes itself right away when it is opened from the second screen:
 * it is relaunched for that screen's size and density, and the relaunch cancels it. Opening it
 * from an invisible activity on the default display avoids that, and shows it on the screen
 * that is about to be captured.
 */
class CaptureConsentActivity : ComponentActivity() {

    private val consent = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val data = result.data
        if (result.resultCode == RESULT_OK && data != null) {
            ScreenCaptureService.start(this, result.resultCode, data)
        }
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState != null) return
        val manager = getSystemService(MediaProjectionManager::class.java)
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            // The game screen is the default display, so skip the "single app" choice.
            manager.createScreenCaptureIntent(MediaProjectionConfig.createConfigForDefaultDisplay())
        } else {
            manager.createScreenCaptureIntent()
        }
        consent.launch(intent)
    }

    companion object {
        fun start(context: Context) {
            val intent = Intent(context, CaptureConsentActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivityOnDisplay(intent, Display.DEFAULT_DISPLAY)
        }
    }
}
