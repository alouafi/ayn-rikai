package io.github.alouafi.ayntranslate.ocr

import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions
import io.github.alouafi.ayntranslate.capture.ScreenImage
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Offline OCR with the Japanese model bundled in the APK. */
class MlKitJapaneseRecognizer : TextRecognizer {

    private val client by lazy { TextRecognition.getClient(JapaneseTextRecognizerOptions.Builder().build()) }

    override suspend fun recognize(screen: ScreenImage): List<String> {
        val text = client.process(InputImage.fromBitmap(screen.bitmap, 0)).await()
        return text.textBlocks.map { block -> block.lines.joinToString("\n") { it.text } }
    }
}

private suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { continuation ->
    addOnCompleteListener { task ->
        val error = task.exception
        when {
            error != null -> continuation.resumeWithException(error)
            task.isCanceled -> continuation.cancel()
            else -> continuation.resume(task.result)
        }
    }
}
