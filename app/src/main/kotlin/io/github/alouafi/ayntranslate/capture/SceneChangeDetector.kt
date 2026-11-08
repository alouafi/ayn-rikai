package io.github.alouafi.ayntranslate.capture

import android.graphics.Bitmap
import androidx.core.graphics.scale
import kotlin.math.abs

/** A small grayscale thumbnail of a frame, cheap to compare. */
class FrameSignature(private val luma: IntArray) {

    /** Share of thumbnail cells whose brightness changed noticeably, from 0 to 1. */
    fun differenceFrom(other: FrameSignature): Float {
        if (other.luma.size != luma.size) return 1f
        val changed = luma.indices.count { abs(luma[it] - other.luma[it]) > CELL_DELTA }
        return changed.toFloat() / luma.size
    }

    companion object {
        const val WIDTH = 64
        const val HEIGHT = 36
        private const val CELL_DELTA = 10

        fun fromArgb(pixels: IntArray): FrameSignature = FrameSignature(IntArray(pixels.size) { luma(pixels[it]) })

        private fun luma(argb: Int): Int {
            val r = argb shr 16 and 0xFF
            val g = argb shr 8 and 0xFF
            val b = argb and 0xFF
            return (299 * r + 587 * g + 114 * b) / 1000
        }
    }
}

fun Bitmap.signature(): FrameSignature {
    val thumbnail = scale(FrameSignature.WIDTH, FrameSignature.HEIGHT)
    val pixels = IntArray(FrameSignature.WIDTH * FrameSignature.HEIGHT)
    thumbnail.getPixels(pixels, 0, FrameSignature.WIDTH, 0, 0, FrameSignature.WIDTH, FrameSignature.HEIGHT)
    if (thumbnail !== this) thumbnail.recycle()
    return FrameSignature.fromArgb(pixels)
}

/**
 * Decides when auto-scan should read the screen: once the picture shows something new
 * and has stopped changing, so text that types out letter by letter is read once, complete.
 */
class SceneChangeDetector(private val threshold: Float = DEFAULT_THRESHOLD) {

    private var previous: FrameSignature? = null
    private var analyzed: FrameSignature? = null

    fun shouldAnalyze(frame: FrameSignature): Boolean {
        val settled = previous?.let { frame.differenceFrom(it) <= threshold } ?: false
        previous = frame
        if (!settled) return false
        return analyzed?.let { frame.differenceFrom(it) > threshold } ?: true
    }

    fun markAnalyzed(frame: FrameSignature) {
        analyzed = frame
    }

    private companion object {
        /** About 12 of the 2,304 cells: a blinking "next" arrow stays below, a new line of dialogue goes above. */
        const val DEFAULT_THRESHOLD = 0.005f
    }
}
