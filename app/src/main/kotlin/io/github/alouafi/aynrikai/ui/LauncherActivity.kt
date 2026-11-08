package io.github.alouafi.aynrikai.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.Display
import io.github.alouafi.aynrikai.display.companionDisplay
import io.github.alouafi.aynrikai.display.startActivityOnDisplay
import io.github.alouafi.aynrikai.ui.companion.CompanionActivity

/**
 * The launcher entry. It opens the companion on the second screen when there is one,
 * whichever screen the app was launched from, then disappears.
 */
class LauncherActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val intent = Intent(this, CompanionActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivityOnDisplay(intent, companionDisplay()?.displayId ?: Display.DEFAULT_DISPLAY)
        finish()
    }
}
