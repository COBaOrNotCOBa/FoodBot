import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

@Serializable
data class OpenAiBotRequest(
    val model: String,
    val messages: List<MessagePromptOpenAiBot>,
    val temperature: Double,
)

@Serializable
data class MessagePromptOpenAiBot(
    val role: String,
    var content: String,
)

@Serializable
data class OpenAiBotResponse(
    @SerialName("id")
    val id: String,
    @SerialName("object")
    val objectOpenAiBot : String,
    @SerialName("created")
    val created : Long,
    @SerialName("model")
    val model: String,
    @SerialName("usage")
    val usage : UsageOpenAiBot,
    @SerialName("choices")
    val choices : List<ChoicesOpenAiBot>,
)

@Serializable
data class UsageOpenAiBot(
    @SerialName("prompt_tokens")
    val promptTokens: Int,
    @SerialName("completion_tokens")
    val compeletionTokens: Int,
    @SerialName("total_tokens")
    val totalTokens: Int,
)

@Serializable
data class ChoicesOpenAiBot(
    @SerialName("message")
    val message: MessageOpenAoBotResponse,
    @SerialName("logprobs")
    val logprobs: Nothing ,
    @SerialName("finish_reason")
    val finishReason: String,
    @SerialName("index")
    val index: Int,
)

@Serializable
data class MessageOpenAoBotResponse(
    @SerialName("role")
    val role: String,
    @SerialName("content")
    val content :String,
)

class OpenAIBot (
    private val json: Json,
    private val tokenOpenAiBot: String,
    ) {

    val openAiBotRequest : OpenAiBotRequest = OpenAiBotRequest(
        model ="gpt-3.5-turbo",
        messages = listOf(MessagePromptOpenAiBot(role = "user", content = "Привет")),
        temperature = 0.7
        )
    fun getOpenAiBotResponse(): OpenAiBotResponse {
        val resultResponse = runCatching { sendOpenAiBotRequest() }.getOrNull() ?: ""
        println(resultResponse)
        println("1")
        return json.decodeFromString(resultResponse)
    }

    private fun sendOpenAiBotRequest(): String {
        try {
            val mediaType = "application/json".toMediaType()
            val openAiBotRequestString = json.encodeToString(openAiBotRequest)
            val body = openAiBotRequestString.toRequestBody(mediaType)
            val request = Request.Builder()
                .url("https://api.openai.com/v1/chat/completions")
                .method("POST", body)
//                .addHeader("Content-Type", "application/json")
//                .addHeader("Accept", "application/json")
                .addHeader("Authorization", "Bearer $tokenOpenAiBot")
                .build()
            val client = OkHttpClient()
            val response = client.newCall(request).execute()
            return response.body?.string() ?: ""
        } catch (e: Exception) {
            println("Ошибка: $e")
            println("sendOpenAiBotRequest")
            return ""
        }
    }
}