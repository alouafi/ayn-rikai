package io.github.alouafi.ayntranslate.settings

import kotlin.math.roundToInt

/** A rectangle in fractions (0..1) of the game screen, so it survives resolution changes. */
data class NormalizedRect(val left: Float, val top: Float, val right: Float, val bottom: Float) {

    val width: Float get() = right - left
    val height: Float get() = bottom - top

    /** Large enough to hold text. Smaller selections are accidental taps. */
    val isUsable: Boolean get() = width >= MIN_SIZE && height >= MIN_SIZE

    fun encode(): String = "$left,$top,$right,$bottom"

    /** The rectangle in pixels of a [width] x [height] image, at least one pixel wide and tall. */
    fun toPixels(width: Int, height: Int): PixelRect {
        val left = (left * width).roundToInt().coerceIn(0, width - 1)
        val top = (top * height).roundToInt().coerceIn(0, height - 1)
        val right = (right * width).roundToInt().coerceIn(left + 1, width)
        val bottom = (bottom * height).roundToInt().coerceIn(top + 1, height)
        return PixelRect(left, top, right - left, bottom - top)
    }

    companion object {
        private const val MIN_SIZE = 0.03f

        /** The rectangle spanned by two corners, in any order, clamped to the screen. */
        fun spanning(x1: Float, y1: Float, x2: Float, y2: Float): NormalizedRect = NormalizedRect(
            left = minOf(x1, x2).coerceIn(0f, 1f),
            top = minOf(y1, y2).coerceIn(0f, 1f),
            right = maxOf(x1, x2).coerceIn(0f, 1f),
            bottom = maxOf(y1, y2).coerceIn(0f, 1f),
        )

        fun decode(value: String): NormalizedRect? {
            val parts = value.split(',').mapNotNull { it.toFloatOrNull() }
            if (parts.size != 4) return null
            return spanning(parts[0], parts[1], parts[2], parts[3]).takeIf { it.isUsable }
        }
    }
}

data class PixelRect(val left: Int, val top: Int, val width: Int, val height: Int)
