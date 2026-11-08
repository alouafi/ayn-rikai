package io.github.alouafi.ayntranslate.net

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import okhttp3.Call
import okhttp3.Callback
import okhttp3.HttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** The server answered, but not with a usable success response. */
class ApiException(val status: Int, message: String) : Exception(message)

private val jsonMediaType = "application/json".toMediaType()

/**
 * POSTs [body] to [url] and returns the decoded JSON object.
 *
 * Throws [ApiException] for non-2xx answers and bodies that are not a JSON object,
 * and [IOException] for network failures.
 */
internal suspend fun OkHttpClient.postJson(url: HttpUrl, apiKey: String, body: JsonObject): JsonObject {
    val request = Request.Builder()
        .url(url)
        .post(body.toString().toRequestBody(jsonMediaType))
        .apply { if (apiKey.isNotBlank()) header("Authorization", "Bearer ${apiKey.trim()}") }
        .build()

    val (status, text) = newCall(request).await().use { response ->
        withContext(Dispatchers.IO) { response.code to response.body.string() }
    }
    if (status !in 200..299) {
        throw ApiException(status, errorMessage(text) ?: "HTTP $status")
    }
    return try {
        Json.parseToJsonElement(text) as? JsonObject
    } catch (_: SerializationException) {
        null
    } ?: throw ApiException(status, "The server did not answer with a JSON object.")
}

/** Pulls a human readable message out of the error formats used by OpenAI, Mistral and FastAPI servers. */
internal fun errorMessage(body: String): String? {
    val root = try {
        Json.parseToJsonElement(body) as? JsonObject
    } catch (_: SerializationException) {
        null
    } ?: return body.trim().take(MAX_MESSAGE_LENGTH).ifBlank { null }

    val error = root["error"]
    val candidates = listOf((error as? JsonObject)?.get("message") ?: error, root["message"], root["detail"])
    return candidates.firstNotNullOfOrNull { it?.describe() }
}

private fun JsonElement.describe(): String? = when (this) {
    is JsonPrimitive -> contentOrNull?.takeIf { it.isNotBlank() }
    else -> toString().take(MAX_MESSAGE_LENGTH)
}

private const val MAX_MESSAGE_LENGTH = 300

internal suspend fun Call.await(): Response = suspendCancellableCoroutine { continuation ->
    continuation.invokeOnCancellation { cancel() }
    enqueue(object : Callback {
        override fun onResponse(call: Call, response: Response) {
            continuation.resume(response) { _, value, _ -> value.close() }
        }

        override fun onFailure(call: Call, e: IOException) {
            continuation.resumeWithException(e)
        }
    })
}
