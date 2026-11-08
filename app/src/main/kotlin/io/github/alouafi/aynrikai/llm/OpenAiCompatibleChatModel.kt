package io.github.alouafi.aynrikai.llm

import io.github.alouafi.aynrikai.net.ApiException
import io.github.alouafi.aynrikai.net.postJson
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
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

    /** Request parameters that some servers or models reject. */
    private enum class Parameter(val key: String) {
        RESPONSE_FORMAT("response_format"),
        TEMPERATURE("temperature"),
        MAX_TOKENS("max_tokens"),
    }

    override suspend fun complete(request: ChatRequest): String {
        val rejected = mutableSetOf<Parameter>()
        while (true) {
            try {
                return send(request, rejected)
            } catch (e: ApiException) {
                // The prompt alone is enough to get JSON, so retry without whatever the server refused.
                val refused = Parameter.entries.filter { it !in rejected && e.rejects(it.key) }
                if (refused.isEmpty()) throw e
                rejected += refused
            }
        }
    }

    private fun ApiException.rejects(key: String): Boolean =
        status in 400..422 && message.orEmpty().contains(key, ignoreCase = true)

    private suspend fun send(request: ChatRequest, rejected: Set<Parameter>): String =
        http.postJson(endpoint, apiKey, requestBody(request, rejected), ChatAnswer.serializer())
            .choices.first().message.text()

    private fun requestBody(request: ChatRequest, rejected: Set<Parameter>): JsonObject = buildJsonObject {
        put("model", model)
        if (Parameter.TEMPERATURE !in rejected) put("temperature", TEMPERATURE)
        // Recent OpenAI models only accept the newer name.
        put(if (Parameter.MAX_TOKENS in rejected) "max_completion_tokens" else "max_tokens", request.maxTokens)
        if (Parameter.RESPONSE_FORMAT !in rejected) putJsonObject("response_format") { put("type", "json_object") }
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

    private companion object {
        /** Low, so readings and translations stay literal. */
        const val TEMPERATURE = 0.2
    }
}

@Serializable
private class ChatAnswer(val choices: List<Choice>) {
    init {
        require(choices.isNotEmpty()) { "The answer has no choices." }
    }
}

@Serializable
private class Choice(val message: Message)

@Serializable
private class Message(val content: JsonElement = JsonNull) {

    /** `content` is a string for most servers, but reasoning and Mistral models may send a list of chunks. */
    fun text(): String = when (content) {
        is JsonPrimitive -> content.contentOrNull.orEmpty()
        is JsonArray -> content.mapNotNull { chunk ->
            val fields = chunk as? JsonObject ?: return@mapNotNull null
            if ((fields["type"] as? JsonPrimitive)?.contentOrNull != "text") return@mapNotNull null
            (fields["text"] as? JsonPrimitive)?.contentOrNull
        }.joinToString("")
        is JsonObject -> ""
    }
}
