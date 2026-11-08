package io.github.alouafi.aynrikai.capture

import android.graphics.Bitmap
import android.media.Image
import androidx.core.graphics.createBitmap
import androidx.core.graphics.scale
import io.github.alouafi.aynrikai.llm.EncodedImage
import io.github.alouafi.aynrikai.settings.NormalizedRect
import java.io.ByteArrayOutputStream
import kotlin.math.roundToInt

/** The part of the frame inside [region], or the whole frame. */
fun Bitmap.crop(region: NormalizedRect?): Bitmap {
    if (region == null) return this
    val pixels = region.toPixels(width, height)
    return Bitmap.createBitmap(this, pixels.left, pixels.top, pixels.width, pixels.height)
}

fun Bitmap.toJpeg(): EncodedImage {
    val factor = uploadScale(width, height)
    val image = if (factor == 1f) this else scale((width * factor).roundToInt(), (height * factor).roundToInt())
    val bytes = ByteArrayOutputStream().use { out ->
        image.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
        out.toByteArray()
    }
    return EncodedImage(bytes, "image/jpeg")
}

/** Copies an RGBA_8888 [Image] into a bitmap, dropping the row padding some GPUs add. */
internal fun Image.toBitmap(): Bitmap {
    val plane = planes[0]
    val rowPixels = plane.rowStride / plane.pixelStride
    val padded = createBitmap(rowPixels, height)
    padded.copyPixelsFromBuffer(plane.buffer)
    if (rowPixels == width) return padded
    return Bitmap.createBitmap(padded, 0, 0, width, height).also { padded.recycle() }
}

private const val JPEG_QUALITY = 90
