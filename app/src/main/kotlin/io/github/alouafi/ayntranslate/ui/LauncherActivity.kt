package io.github.alouafi.ayntranslate.ui

import android.app.Activity
import android.app.ActivityOptions
import android.content.Intent
import android.os.Bundle
import io.github.alouafi.ayntranslate.display.DisplayRouter
import io.github.alouafi.ayntranslate.ui.companion.CompanionActivity

/**
 * The launcher entry. It opens the companion on the second screen when there is one,
 * whichever screen the app was launched from, then disappears.
 */
class LauncherActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val intent = Intent(this, CompanionActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val display = DisplayRouter.companionDisplay(this)
        try {
            startActivity(intent, display?.let { ActivityOptions.makeBasic().setLaunchDisplayId(it.displayId).toBundle() })
        } catch (_: SecurityException) {
            // Some systems do not let apps pick a display. The default screen still works.
            startActivity(intent)
        }
        finish()
    }
}
