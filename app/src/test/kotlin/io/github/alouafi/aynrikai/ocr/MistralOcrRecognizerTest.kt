package io.github.alouafi.aynrikai.ocr

import io.github.alouafi.aynrikai.capture.FakeScreenImage
import io.github.alouafi.aynrikai.llm.EncodedImage
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class MistralOcrRecognizerTest {

    private val server = MockWebServer()

    @Before
    fun start() = server.start()

    @After
    fun stop() = server.close()

    @Test
    fun `sends the image and splits the pages into text blocks`() = runTest {
        val pages = """{"pages":[{"index":0,"markdown":"# 村長\n\nよく来たな、\n勇者よ。\n\n![img-0.jpeg](img-0.jpeg)"}],"model":"mistral-ocr-latest"}"""
        server.enqueue(MockResponse.Builder().code(200).body(pages).build())
        val recognizer = MistralOcrRecognizer(OkHttpClient(), server.url("/v1"), "secret")

        val blocks = recognizer.recognize(FakeScreenImage(EncodedImage(byteArrayOf(1, 2, 3), "image/jpeg")))

        assertEquals(listOf("村長", "よく来たな、\n勇者よ。"), blocks)
        val request = server.takeRequest()
        assertEquals("/v1/ocr", request.url.encodedPath)
        assertEquals("Bearer secret", request.headers["Authorization"])
        val body = Json.parseToJsonElement(request.body!!.utf8()).jsonObject
        assertEquals("mistral-ocr-latest", body["model"]!!.jsonPrimitive.content)
        val document = body["document"]!!.jsonObject
        assertEquals("image_url", document["type"]!!.jsonPrimitive.content)
        assertEquals("data:image/jpeg;base64,AQID", document["image_url"]!!.jsonPrimitive.content)
    }

    @Test
    fun `markdown blocks drop images, headings and bold markers`() {
        val markdown = "## **HP** 30/30\n\n  \n**はい**\nいいえ\n\n![a](a.png)\n\n   \n"

        assertEquals(listOf("HP 30/30", "はい\nいいえ"), markdownBlocks(markdown))
    }
}
