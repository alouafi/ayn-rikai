package io.github.alouafi.ayntranslate.ocr

import io.github.alouafi.ayntranslate.capture.ScreenImage
import io.github.alouafi.ayntranslate.net.postJson
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import okhttp3.HttpUrl
import okhttp3.OkHttpClient

/** Mistral's `/v1/ocr` endpoint, which answers with one markdown document per page. */
class MistralOcrRecognizer(
    private val http: OkHttpClient,
    baseUrl: HttpUrl,
    private val apiKey: String,
) : TextRecognizer {

    private val endpoint = baseUrl.newBuilder().addPathSegment("ocr").build()

    override suspend fun recognize(screen: ScreenImage): List<String> {
        val body = buildJsonObject {
            put("model", MODEL)
            putJsonObject("document") {
                put("type", "image_url")
                put("image_url", screen.jpeg().dataUrl())
            }
        }
        val answer = http.postJson(endpoint, apiKey, body, OcrAnswer.serializer())
        return markdownBlocks(answer.pages.joinToString("\n\n") { it.markdown })
    }

    private companion object {
        const val MODEL = "mistral-ocr-latest"
    }
}

@Serializable
private class OcrAnswer(val pages: List<Page>)

@Serializable
private class Page(val markdown: String = "")

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
