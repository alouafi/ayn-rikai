package io.github.alouafi.aynrikai.capture

import android.graphics.Bitmap
import io.github.alouafi.aynrikai.llm.EncodedImage

/** A screen whose JPEG is fixed. Its bitmap is unavailable in JVM tests. */
class FakeScreenImage(private val image: EncodedImage = EncodedImage(byteArrayOf(1, 2, 3), "image/jpeg")) : ScreenImage {
    override val bitmap: Bitmap get() = throw UnsupportedOperationException("No bitmaps in JVM tests")

    override suspend fun jpeg(): EncodedImage = image
}
