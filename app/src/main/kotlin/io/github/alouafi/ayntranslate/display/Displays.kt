package io.github.alouafi.ayntranslate.display

import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.hardware.display.DisplayManager
import android.view.Display

/**
 * The screen for the companion. On dual-screen handhelds such as the AYN Thor, the game runs on
 * the default display and the second built-in screen is free for the companion. Returns the first
 * secondary screen that is on, or null on single-screen devices.
 */
fun Context.companionDisplay(): Display? =
    getSystemService(DisplayManager::class.java).displays.firstOrNull { display ->
        display.displayId != Display.DEFAULT_DISPLAY &&
            display.isValid &&
            display.flags and Display.FLAG_PRIVATE == 0 &&
            display.state != Display.STATE_OFF
    }

/** Starts [intent] on [displayId], or wherever the system decides when it does not let apps pick a display. */
fun Context.startActivityOnDisplay(intent: Intent, displayId: Int) {
    val options = ActivityOptions.makeBasic().setLaunchDisplayId(displayId).toBundle()
    try {
        startActivity(intent, options)
    } catch (_: SecurityException) {
        startActivity(intent)
    }
}
