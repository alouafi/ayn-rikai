package io.github.alouafi.ayntranslate.llm

import io.github.alouafi.ayntranslate.net.ApiException
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.RecordedRequest
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class OpenAiCompatibleChatModelTest {

    private val server = MockWebServer()
    private val http = OkHttpClient()

    @Before
    fun start() = server.start()

    @After
    fun stop() = server.close()

    private fun model(apiKey: String = "secret", basePath: String = "/v1") =
        OpenAiCompatibleChatModel(http, server.url(basePath), apiKey, "test-model")

    private fun answer(content: String) = MockResponse.Builder()
        .code(200)
        .body("""{"choices":[{"message":{"role":"assistant","content":$content}}]}""")
        .build()

    private fun error(code: Int, body: String) = MockResponse.Builder().code(code).body(body).build()

    private fun RecordedRequest.json(): JsonObject = Json.parseToJsonElement(body!!.utf8()).jsonObject

    private val textRequest = ChatRequest(system = "Be brief.", user = "ping")

    private suspend fun apiError(): ApiException = try {
        model().complete(textRequest)
        throw AssertionError("Expected an ApiException")
    } catch (e: ApiException) {
        e
    }

    @Test
    fun `sends an OpenAI chat request with the image as a data URL`() = runTest {
        server.enqueue(answer("\"{}\""))
        val image = EncodedImage(byteArrayOf(1, 2, 3), "image/jpeg")

        model().complete(ChatRequest(system = "Read the screen.", user = "Go", image = image, maxTokens = 100))

        val request = server.takeRequest()
        assertEquals("/v1/chat/completions", request.url.encodedPath)
        assertEquals("Bearer secret", request.headers["Authorization"])
        val body = request.json()
        assertEquals("test-model", body["model"]!!.jsonPrimitive.content)
        assertEquals("100", body["max_tokens"]!!.jsonPrimitive.content)
        assertEquals("json_object", body["response_format"]!!.jsonObject["type"]!!.jsonPrimitive.content)
        val messages = body["messages"]!!.jsonArray
        assertEquals("Read the screen.", messages[0].jsonObject["content"]!!.jsonPrimitive.content)
        val parts = messages[1].jsonObject["content"]!!.jsonArray
        assertEquals("Go", parts[0].jsonObject["text"]!!.jsonPrimitive.content)
        assertEquals(
            "data:image/jpeg;base64,AQID",
            parts[1].jsonObject["image_url"]!!.jsonObject["url"]!!.jsonPrimitive.content,
        )
    }

    @Test
    fun `works with a trailing slash and without a key`() = runTest {
        server.enqueue(answer("\"pong\""))

        val text = model(apiKey = "", basePath = "/v1/").complete(textRequest)

        assertEquals("pong", text)
        val request = server.takeRequest()
        assertEquals("/v1/chat/completions", request.url.encodedPath)
        assertNull(request.headers["Authorization"])
        assertTrue(request.json()["messages"]!!.jsonArray[1].jsonObject["content"] !is JsonArray)
    }

    @Test
    fun `joins text chunks of a chunked answer`() = runTest {
        server.enqueue(
            answer("""[{"type":"thinking","thinking":[]},{"type":"text","text":"{\"lines\""},{"type":"text","text":":[]}"}]"""),
        )

        assertEquals("""{"lines":[]}""", model().complete(textRequest))
    }

    @Test
    fun `retries without the parameters a server rejects`() = runTest {
        server.enqueue(error(400, """{"error":{"message":"Unsupported parameter: 'max_tokens'. Use 'max_completion_tokens'."}}"""))
        server.enqueue(error(400, """{"error":{"message":"response_format is not supported by this model"}}"""))
        server.enqueue(error(422, """{"detail":"temperature must be 1"}"""))
        server.enqueue(answer("\"ok\""))

        assertEquals("ok", model().complete(textRequest))

        val first = server.takeRequest().json()
        assertTrue(first.containsKey("max_tokens") && first.containsKey("response_format") && first.containsKey("temperature"))
        val second = server.takeRequest().json()
        assertFalse(second.containsKey("max_tokens"))
        assertEquals("4096", second["max_completion_tokens"]!!.jsonPrimitive.content)
        assertFalse(server.takeRequest().json().containsKey("response_format"))
        val last = server.takeRequest().json()
        assertFalse(last.containsKey("response_format") || last.containsKey("temperature"))
        assertTrue(last.containsKey("max_completion_tokens"))
    }

    @Test
    fun `reports server errors with their message`() = runTest {
        server.enqueue(error(401, """{"message":"Unauthorized","request_id":"abc"}"""))

        val error = apiError()

        assertEquals(401, error.status)
        assertEquals("Unauthorized", error.message)
        assertEquals(1, server.requestCount)
    }

    @Test
    fun `does not retry errors unrelated to optional parameters`() = runTest {
        server.enqueue(error(404, """{"error":{"message":"The model `test-model` does not exist","type":"invalid_request_error"}}"""))

        val error = apiError()

        assertEquals("The model `test-model` does not exist", error.message)
        assertEquals(1, server.requestCount)
    }

    @Test
    fun `an answer without choices is an API error`() = runTest {
        server.enqueue(MockResponse.Builder().code(200).body("""{"object":"error"}""").build())
        server.enqueue(MockResponse.Builder().code(200).body("""{"choices":[]}""").build())

        assertEquals(200, apiError().status)
        assertEquals(200, apiError().status)
    }
}
