package io.github.alouafi.aynrikai.capture

/**
 * Scale for images sent to models: large screens are shrunk to save upload time,
 * small crops are enlarged so their text stays legible to vision encoders.
 */
internal fun uploadScale(width: Int, height: Int): Float {
    val longSide = maxOf(width, height).toFloat()
    return when {
        longSide > MAX_UPLOAD_SIDE -> MAX_UPLOAD_SIDE / longSide
        longSide < MIN_UPLOAD_SIDE -> MIN_UPLOAD_SIDE / longSide
        else -> 1f
    }
}

private const val MAX_UPLOAD_SIDE = 1600f
private const val MIN_UPLOAD_SIDE = 768f
