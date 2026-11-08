package io.github.alouafi.ayntranslate.llm

import java.util.Base64

/** A chat completion endpoint that can optionally look at one image. It is always asked for JSON. */
fun interface ChatModel {
    suspend fun complete(request: ChatRequest): String
}

data class ChatRequest(
    val system: String,
    val user: String,
    val image: EncodedImage? = null,
    val maxTokens: Int = 4096,
)

class EncodedImage(val bytes: ByteArray, val mimeType: String) {
    fun dataUrl(): String = "data:$mimeType;base64," + Base64.getEncoder().encodeToString(bytes)
}
