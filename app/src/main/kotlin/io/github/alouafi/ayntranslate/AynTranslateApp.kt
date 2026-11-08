package io.github.alouafi.ayntranslate

import android.app.Application
import android.content.Context

class AynTranslateApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

val Context.appContainer: AppContainer get() = (applicationContext as AynTranslateApp).container
