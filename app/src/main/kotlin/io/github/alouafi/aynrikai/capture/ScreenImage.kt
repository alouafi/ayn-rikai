package io.github.alouafi.aynrikai.capture

import android.graphics.Bitmap
import io.github.alouafi.aynrikai.llm.EncodedImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** One captured game screen, cropped to the reading region. */
interface ScreenImage {
    /** The pixels, for on-device OCR. */
    val bitmap: Bitmap

    /** The pixels as a JPEG sized for vision models and OCR APIs. */
    suspend fun jpeg(): EncodedImage
}

class BitmapScreenImage(override val bitmap: Bitmap) : ScreenImage {
    override suspend fun jpeg(): EncodedImage = withContext(Dispatchers.Default) { bitmap.toJpeg() }
}
