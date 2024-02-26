import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

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


@Serializable
data class RequestGpt(
    @SerialName("modelUri")
    val modelUri: String = "gpt://b1gidtrrq0kiv3kf31u2/yandexgpt-lite",
    @SerialName("completionOptions")
    val completionOptions: CompletionOptions,
    @SerialName("messages")
    val messages: List<Role>,
)

@Serializable
data class CompletionOptions(
    @SerialName("stream")
    val stream: Boolean = false,
    @SerialName("temperature")
    val temperature: Double = 0.6,
    @SerialName("maxTokens")
    val maxTokens: String = "2000",
)

@Serializable
data class Role(
    @SerialName("role")
    val role: String,
    @SerialName("text")
    val text: String,
)

class GptBot(
    private val json: Json,
    private val botToken: String,
    private val requestGpt: String,
) {

    private val folderId: String = "ajejmadk7ai886qpha8e"
//    private val promptJson = json.encodeToString(requestGpt)

    fun getUpdateGpt(): ResponseGpt {
        val resultGpt = runCatching { sendGptRequest() }.getOrNull() ?: ""
        println(resultGpt)
        return json.decodeFromString(resultGpt)
    }

    private fun sendGptRequest(): String {
        val gptUrl = "https://llm.api.cloud.yandex.net/foundationModels/v1/completion"
        val client = OkHttpClient()
        val requestBody = requestGpt.toRequestBody("application/json".toMediaType())
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