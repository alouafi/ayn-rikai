package io.github.alouafi.ayntranslate.ocr

import io.github.alouafi.ayntranslate.analysis.ScreenImage
import io.github.alouafi.ayntranslate.net.ApiException
import io.github.alouafi.ayntranslate.net.postJson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import okhttp3.HttpUrl
import okhttp3.OkHttpClient

/** Mistral's `/v1/ocr` endpoint, which answers with one markdown document per page. */
class MistralOcrRecognizer(
    private val http: OkHttpClient,
    baseUrl: HttpUrl,
    private val apiKey: String,
    private val model: String,
) : TextRecognizer {

    private val endpoint = baseUrl.newBuilder().addPathSegment("ocr").build()

    override suspend fun recognize(screen: ScreenImage): List<String> {
        val image = withContext(Dispatchers.Default) { screen.jpeg() }
        val body = buildJsonObject {
            put("model", model)
            putJsonObject("document") {
                put("type", "image_url")
                put("image_url", image.dataUrl())
            }
        }
        val pages = http.postJson(endpoint, apiKey, body)["pages"] as? JsonArray
            ?: throw ApiException(200, "The OCR answer has no pages.")
        val markdown = pages.joinToString("\n\n") { page ->
            ((page as? JsonObject)?.get("markdown") as? JsonPrimitive)?.contentOrNull.orEmpty()
        }
        return markdownBlocks(markdown)
    }
}

private val IMAGE = Regex("""!\[[^\]]*]\([^)]*\)""")
private val HEADING = Regex("""^\s{0,3}#{1,6}\s+""")
private val BLANK_LINES = Regex("""\n\s*\n""")

/** Splits OCR markdown into text blocks, dropping image references and markdown markup. */
internal fun markdownBlocks(markdown: String): List<String> =
    markdown.split(BLANK_LINES)
        .map { block ->
            block.lines()
                .map { row -> row.replace(IMAGE, "").replace(HEADING, "").replace("**", "").trim() }
                .filter { it.isNotEmpty() }
                .joinToString("\n")
        }
        .filter { it.isNotEmpty() }
