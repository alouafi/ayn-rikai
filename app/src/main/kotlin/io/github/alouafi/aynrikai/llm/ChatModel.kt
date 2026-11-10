package io.github.alouafi.aynrikai.llm

import java.util.Base64

/** A chat completion endpoint that can optionally look at one image. It is always asked for JSON. */
fun interface ChatModel {
    /** Throws [TruncatedAnswerException] when the answer reached [ChatRequest.maxTokens]. */
    suspend fun complete(request: ChatRequest): String
}

data class ChatRequest(
    val system: String,
    val user: String,
    val image: EncodedImage? = null,
    /**
     * The full analysis of a screen full of text takes about 4,000 tokens, and reasoning counts too, so the cap
     * only stops runaway answers.
     */
    val maxTokens: Int = 16_384,
)

class EncodedImage(val bytes: ByteArray, val mimeType: String) {
    fun dataUrl(): String = "data:$mimeType;base64," + Base64.getEncoder().encodeToString(bytes)
}

/** The answer stopped at the token limit, so its JSON is cut off. */
class TruncatedAnswerException : Exception("The answer reached the token limit.")
