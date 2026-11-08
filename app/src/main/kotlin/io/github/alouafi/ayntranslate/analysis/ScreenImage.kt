package io.github.alouafi.ayntranslate.analysis

import android.graphics.Bitmap
import io.github.alouafi.ayntranslate.capture.toJpeg
import io.github.alouafi.ayntranslate.llm.EncodedImage

/** One captured game screen, cropped to the reading region. */
interface ScreenImage {
    /** The pixels, for on-device OCR. */
    val bitmap: Bitmap

    /** The pixels as a JPEG sized for vision models and OCR APIs. */
    fun jpeg(): EncodedImage
}

class BitmapScreenImage(override val bitmap: Bitmap) : ScreenImage {
    private val encoded by lazy { bitmap.toJpeg() }

    override fun jpeg(): EncodedImage = encoded
}
