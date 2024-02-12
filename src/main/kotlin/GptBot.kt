import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

@Serializable
data class ResponseGpt(
    @SerialName("result")
    val result: Alternatives,
)

@Serializable
data class Alternatives(
    @SerialName("alternatives")
    val alternatives: List<MessageGpt>,
)

@Serializable
data class MessageGpt(
    @SerialName("message")
    val message: TextGpt,
)

@Serializable
data class TextGpt(
    @SerialName("text")
    val text: String,
)

class GptBot(
    private val botToken: String,
    private val folderId: String,
    private val promptFilePath: String,
    private val json: Json
) {

    fun getUpdateGpt(): ResponseGpt {
        val resultGpt = runCatching { sendGptRequest() }.getOrNull() ?: ""
        println(resultGpt)
        return json.decodeFromString(resultGpt)
    }

    private fun sendGptRequest(): String {
        val gptUrl = "https://llm.api.cloud.yandex.net/foundationModels/v1/completion"
        val promptFile = File(promptFilePath)
        val promptJson = promptFile.readText()
        val client = OkHttpClient()
        val requestBody = promptJson.toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url(gptUrl)
            .header("Content-Type", "application/json")
            .header("Authorization", "Api-Key $botToken")
            .header("x-folder-id", folderId)
            .post(requestBody)
            .build()
        val response = client.newCall(request).execute()
        return response.body?.string() ?: ""
    }
}