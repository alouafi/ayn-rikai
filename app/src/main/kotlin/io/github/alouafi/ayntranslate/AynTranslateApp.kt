package io.github.alouafi.ayntranslate

import android.app.Application

class AynTranslateApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
