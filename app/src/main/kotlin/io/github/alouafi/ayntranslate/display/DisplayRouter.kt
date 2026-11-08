package io.github.alouafi.ayntranslate.display

import android.content.Context
import android.hardware.display.DisplayManager
import android.view.Display

/**
 * Picks the screen for the companion. On dual-screen handhelds such as the AYN Thor, the
 * game runs on the default display and the second built-in screen is free for the companion.
 */
object DisplayRouter {

    /** The first secondary screen that is on, or null on single-screen devices. */
    fun companionDisplay(context: Context): Display? =
        context.getSystemService(DisplayManager::class.java).displays.firstOrNull { display ->
            display.displayId != Display.DEFAULT_DISPLAY &&
                display.isValid &&
                display.flags and Display.FLAG_PRIVATE == 0 &&
                display.state != Display.STATE_OFF
        }
}
