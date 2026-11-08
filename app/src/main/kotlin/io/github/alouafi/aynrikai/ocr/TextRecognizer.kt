package io.github.alouafi.aynrikai.ocr

import io.github.alouafi.aynrikai.capture.ScreenImage

fun interface TextRecognizer {
    /** Returns the text blocks found on screen, in reading order. Rows of a block are separated by `\n`. */
    suspend fun recognize(screen: ScreenImage): List<String>
}
