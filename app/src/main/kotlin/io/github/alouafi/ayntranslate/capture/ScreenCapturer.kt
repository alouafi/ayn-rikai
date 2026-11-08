package io.github.alouafi.ayntranslate.capture

import android.content.Context
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.os.Handler
import android.os.HandlerThread
import android.view.Display
import android.view.Surface
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import java.io.Closeable
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** No frame to read right now. Auto-scan skips the tick; a manual scan tells the user. */
sealed class FrameUnavailableException(message: String) : IllegalStateException(message)

class CaptureStoppedException : FrameUnavailableException("Screen capture stopped.")

class NoFrameException : FrameUnavailableException("The screen sent no frame in time.")

/**
 * Grabs frames of the default display, which is the top screen on dual-screen handhelds.
 *
 * The mirror display only gets a surface while a frame is requested, so the compositor does not
 * render a second copy of the game between scans. Android 14 allows one virtual display per
 * projection, so it is created once and resized when the screen changes.
 */
class ScreenCapturer(context: Context, projection: MediaProjection) : Closeable {

    private val displayManager = context.getSystemService(DisplayManager::class.java)
    private val densityDpi = context.resources.displayMetrics.densityDpi
    private val thread = HandlerThread("ScreenCapturer").apply { start() }
    private val handler = Handler(thread.looper)
    private val mutex = Mutex()

    private var size = defaultDisplaySize()
    private var reader = newReader(size)
    private val mirror: VirtualDisplay = projection.createVirtualDisplay(
        "ayn-translate",
        size.width,
        size.height,
        densityDpi,
        DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
        null,
        null,
        handler,
    ) ?: throw CaptureStoppedException()

    @Volatile
    private var closed = false

    /** Captures the next frame of the game screen. Must be called from the main thread. */
    suspend fun capture(): Bitmap = mutex.withLock {
        if (closed) throw CaptureStoppedException()
        resizeIfNeeded()
        try {
            withTimeoutOrNull(FRAME_TIMEOUT_MS) { awaitFrame() } ?: throw NoFrameException()
        } finally {
            reader.setOnImageAvailableListener(null, null)
            if (!closed) mirror.surface = null
        }
    }

    private suspend fun awaitFrame(): Bitmap = suspendCancellableCoroutine { continuation ->
        reader.acquireLatestImage()?.close()
        reader.setOnImageAvailableListener({ source ->
            val image = source.acquireLatestImage() ?: return@setOnImageAvailableListener
            source.setOnImageAvailableListener(null, null)
            val bitmap = try {
                image.use { it.toBitmap() }
            } catch (e: RuntimeException) {
                continuation.resumeWithException(e)
                return@setOnImageAvailableListener
            }
            continuation.resume(bitmap) { _, value, _ -> value.recycle() }
        }, handler)
        mirror.surface = reader.surface
    }

    private fun resizeIfNeeded() {
        val current = defaultDisplaySize()
        if (current == size) return
        size = current
        reader.close()
        reader = newReader(current)
        mirror.resize(current.width, current.height, densityDpi)
    }

    private fun defaultDisplaySize(): CaptureSize {
        val display = displayManager.getDisplay(Display.DEFAULT_DISPLAY)
        val mode = display.mode
        val sideways = display.rotation == Surface.ROTATION_90 || display.rotation == Surface.ROTATION_270
        return if (sideways) {
            CaptureSize(mode.physicalHeight, mode.physicalWidth)
        } else {
            CaptureSize(mode.physicalWidth, mode.physicalHeight)
        }
    }

    private fun newReader(size: CaptureSize): ImageReader =
        ImageReader.newInstance(size.width, size.height, PixelFormat.RGBA_8888, MAX_IMAGES)

    override fun close() {
        if (closed) return
        closed = true
        mirror.release()
        reader.close()
        thread.quitSafely()
    }

    private data class CaptureSize(val width: Int, val height: Int)

    private companion object {
        const val FRAME_TIMEOUT_MS = 1_500L
        const val MAX_IMAGES = 2
    }
}
