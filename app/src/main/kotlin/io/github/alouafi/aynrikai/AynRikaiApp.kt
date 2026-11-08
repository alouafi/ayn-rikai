package io.github.alouafi.aynrikai

import android.app.Application
import android.content.Context

class AynRikaiApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

val Context.appContainer: AppContainer get() = (applicationContext as AynRikaiApp).container
