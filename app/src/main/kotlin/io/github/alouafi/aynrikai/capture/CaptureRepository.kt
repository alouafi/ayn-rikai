package io.github.alouafi.aynrikai.capture

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Shares the running [ScreenCapturer] between the capture service and the UI. */
class CaptureRepository {
    private val current = MutableStateFlow<ScreenCapturer?>(null)
    val capturer: StateFlow<ScreenCapturer?> = current.asStateFlow()

    internal fun publish(capturer: ScreenCapturer?) {
        current.value = capturer
    }
}
