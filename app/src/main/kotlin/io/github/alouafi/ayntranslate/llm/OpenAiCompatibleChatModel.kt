package io.github.alouafi.ayntranslate.llm

import io.github.alouafi.ayntranslate.net.ApiException
import io.github.alouafi.ayntranslate.net.postJson
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import okhttp3.HttpUrl
import okhttp3.OkHttpClient

/**
 * Talks to any `/chat/completions` endpoint that follows the OpenAI format:
 * Mistral, OpenAI, OpenRouter, Ollama, LM Studio, vLLM, llama.cpp...
 */
class OpenAiCompatibleChatModel(
    private val http: OkHttpClient,
    baseUrl: HttpUrl,
    private val apiKey: String,
    private val model: String,
) : ChatModel {

    private val endpoint = baseUrl.newBuilder().addPathSegments("chat/completions").build()

    /** Optional parameters that some servers or models reject. */
    private enum class Option(val parameter: String) {
        JSON_MODE("response_format"),
        TEMPERATURE("temperature"),
        MAX_TOKENS("max_tokens"),
    }

    override suspend fun complete(request: ChatRequest): String {
        val dropped = mutableSetOf<Option>()
        if (!request.jsonOutput) dropped += Option.JSON_MODE
        while (true) {
            try {
                return send(request, dropped)
            } catch (e: ApiException) {
                // The prompt alone is enough to get JSON, so retry without whatever the server refused.
                val rejected = Option.entries.filter { it !in dropped && e.rejects(it.parameter) }
                if (rejected.isEmpty()) throw e
                dropped += rejected
            }
        }
    }

    private fun ApiException.rejects(parameter: String): Boolean =
        status in 400..422 && message.orEmpty().contains(parameter, ignoreCase = true)

    private suspend fun send(request: ChatRequest, dropped: Set<Option>): String {
        val response = http.postJson(endpoint, apiKey, requestBody(request, dropped))
        val message = (response["choices"] as? JsonArray)?.firstOrNull()?.jsonObject?.get("message") as? JsonObject
            ?: throw ApiException(200, "The answer has no choices.")
        return messageText(message)
    }

    private fun requestBody(request: ChatRequest, dropped: Set<Option>): JsonObject = buildJsonObject {
        put("model", model)
        if (Option.TEMPERATURE !in dropped) put("temperature", request.temperature)
        // Recent OpenAI models only accept the newer name.
        put(if (Option.MAX_TOKENS in dropped) "max_completion_tokens" else "max_tokens", request.maxTokens)
        if (Option.JSON_MODE !in dropped) putJsonObject("response_format") { put("type", "json_object") }
        putJsonArray("messages") {
            addJsonObject {
                put("role", "system")
                put("content", request.system)
            }
            addJsonObject {
                put("role", "user")
                if (request.image == null) {
                    put("content", request.user)
                } else {
                    putJsonArray("content") {
                        addJsonObject {
                            put("type", "text")
                            put("text", request.user)
                        }
                        addJsonObject {
                            put("type", "image_url")
                            putJsonObject("image_url") { put("url", request.image.dataUrl()) }
                        }
                    }
                }
            }
        }
    }

    /** `content` is a string for most servers, but reasoning and Mistral models may send a list of chunks. */
    private fun messageText(message: JsonObject): String = when (val content = message["content"]) {
        is JsonPrimitive -> content.contentOrNull.orEmpty()
        is JsonArray -> content.mapNotNull { chunk ->
            val fields = chunk as? JsonObject ?: return@mapNotNull null
            if ((fields["type"] as? JsonPrimitive)?.contentOrNull != "text") return@mapNotNull null
            (fields["text"] as? JsonPrimitive)?.contentOrNull
        }.joinToString("")
        else -> ""
    }
}
